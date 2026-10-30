package com.example.ocr

import com.example.data.model.ShipmentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ExtractedShipment(
    val serialNumber: String,
    val awbNumber: String,
    val courierPartner: String,
    val consigneeName: String,
    val consigneePhone: String,
    val addressLine: String,
    val city: String,
    val state: String,
    val pincode: String,
    val country: String,
    val shipperName: String,
    val referenceNumber: String,
    val weight: String,
    val rawText: String,
    val detectedLines: List<String>
)

object LogisticsDataExtractor {

    private val COURIER_PATTERNS = listOf(
        "Delhivery" to Pattern.compile("(?i)\\b(delhivery)\\b"),
        "Blue Dart" to Pattern.compile("(?i)\\b(blue\\s*dart|bluedart)\\b"),
        "DTDC" to Pattern.compile("(?i)\\b(dtdc)\\b"),
        "India Post / Speed Post" to Pattern.compile("(?i)\\b(speed\\s*post|india\\s*post|indiapost)\\b"),
        "FedEx" to Pattern.compile("(?i)\\b(fedex|federal\\s*express)\\b"),
        "DHL" to Pattern.compile("(?i)\\b(dhl|dhl\\s*express)\\b"),
        "Ekart Logistics" to Pattern.compile("(?i)\\b(ekart|fmpc)\\b"),
        "Shadowfax" to Pattern.compile("(?i)\\b(shadowfax|sfx)\\b"),
        "Xpressbees" to Pattern.compile("(?i)\\b(xpressbees)\\b"),
        "Ecom Express" to Pattern.compile("(?i)\\b(ecom\\s*express)\\b"),
        "Amazon Shipping" to Pattern.compile("(?i)\\b(amazon\\s*shipping|ats)\\b"),
        "UPS" to Pattern.compile("(?i)\\b(ups|united\\s*parcel)\\b")
    )

    private val INDIAN_STATES = listOf(
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
        "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
        "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
        "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
        "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
        "Uttar Pradesh", "Uttarakhand", "West Bengal", "Delhi", "Chandigarh",
        "Puducherry", "Jammu and Kashmir", "Ladakh"
    )

    private val INDIAN_CITIES = listOf(
        "Mumbai", "Delhi", "Bengaluru", "Bangalore", "Hyderabad", "Ahmedabad",
        "Chennai", "Kolkata", "Surat", "Pune", "Jaipur", "Lucknow", "Kanpur",
        "Nagpur", "Indore", "Thane", "Bhopal", "Visakhapatnam", "Pimpri-Chinchwad",
        "Patna", "Vadodara", "Ghaziabad", "Ludhiana", "Agra", "Nashik", "Faridabad",
        "Meerut", "Rajkot", "Kalyan", "Varanasi", "Srinagar", "Aurangabad", "Dhanbad",
        "Amritsar", "Navi Mumbai", "Allahabad", "Prayagraj", "Ranchi", "Howrah",
        "Coimbatore", "Jabalpur", "Gwalior", "Vijayawada", "Jodhpur", "Madurai",
        "Raipur", "Kota", "Guwahati", "Chandigarh", "Solapur", "Hubli", "Dharwad",
        "Bareilly", "Mysuru", "Mysore", "Tiruchirappalli", "Salem", "Aligarh",
        "Gurgaon", "Gurugram", "Noida", "Kochi", "Ernakulam", "Trivandrum",
        "Thiruvananthapuram", "Dehradun", "Bhubaneswar", "Mangaluru", "Mangalore",
        "Cuttack", "Udaipur", "Jammu", "Panaji"
    )

    fun extract(rawText: String, fallbackSerialIndex: Int = 1): ExtractedShipment {
        val lines = rawText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var courier = detectCourier(rawText)
        var awb = extractAwb(rawText, lines)
        val pincode = extractPincode(rawText, lines)
        val phone = extractPhone(rawText, lines)
        val weight = extractWeight(rawText, lines)
        val refNo = extractReference(rawText, lines)
        val state = extractState(rawText)
        val city = extractCity(rawText, state)
        val consigneeName = extractConsigneeName(lines)
        val shipperName = extractShipperName(lines)
        val address = extractAddress(lines, consigneeName, city, pincode)

        val datePrefix = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val serialNumber = "SN-$datePrefix-%03d".format(fallbackSerialIndex)

        // Refine courier if not detected by brand name but AWB format matches
        if (courier == "Generic Courier") {
            courier = inferCourierFromAwb(awb)
        }

        return ExtractedShipment(
            serialNumber = serialNumber,
            awbNumber = awb,
            courierPartner = courier,
            consigneeName = consigneeName,
            consigneePhone = phone,
            addressLine = address,
            city = city,
            state = state,
            pincode = pincode,
            country = "India",
            shipperName = shipperName,
            referenceNumber = refNo,
            weight = weight,
            rawText = rawText,
            detectedLines = lines
        )
    }

    private fun detectCourier(text: String): String {
        for ((name, pattern) in COURIER_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return name
            }
        }
        return "Generic Courier"
    }

    private fun inferCourierFromAwb(awb: String): String {
        val cleanAwb = awb.replace("[\\s-]".toRegex(), "").uppercase(Locale.US)
        return when {
            cleanAwb.matches("^[A-Z]{2}\\d{9}IN$".toRegex()) -> "Speed Post (India Post)"
            cleanAwb.startsWith("FMPC") || cleanAwb.startsWith("FMPP") -> "Ekart Logistics"
            cleanAwb.startsWith("1Z") && cleanAwb.length == 18 -> "UPS"
            cleanAwb.matches("^[BDKPZ]\\d{8,9}$".toRegex()) -> "DTDC"
            cleanAwb.matches("^\\d{12}$|^\\d{14}$".toRegex()) -> "Delhivery"
            cleanAwb.matches("^\\d{11}$".toRegex()) -> "Blue Dart"
            else -> "Standard Courier"
        }
    }

    private fun extractAwb(text: String, lines: List<String>): String {
        // 1. Look for labeled AWB / Tracking prefix
        val labeledRegex = Pattern.compile(
            "(?i)(?:AWB|TRACKING|TRACK\\s*NO|WAYBILL|CN\\s*NO|CONSIGNMENT|LR\\s*NO|POD)[\\s.:#-]+([A-Z0-9]{8,22})"
        )
        val matcher = labeledRegex.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }

        // 2. Look for Speed post pattern e.g. EM123456789IN
        val speedPostRegex = Pattern.compile("\\b([A-Z]{2}\\d{9}IN)\\b", Pattern.CASE_INSENSITIVE)
        val spMatcher = speedPostRegex.matcher(text)
        if (spMatcher.find()) {
            return spMatcher.group(1)?.uppercase(Locale.US) ?: ""
        }

        // 3. Look for Ekart pattern FMPC1234567890
        val ekartRegex = Pattern.compile("\\b(FMP[CP]\\d{9,12})\\b", Pattern.CASE_INSENSITIVE)
        val ekartMatcher = ekartRegex.matcher(text)
        if (ekartMatcher.find()) {
            return ekartMatcher.group(1)?.uppercase(Locale.US) ?: ""
        }

        // 4. Look for DTDC pattern (Letter + 8 digits)
        val dtdcRegex = Pattern.compile("\\b([BDKPZ]\\d{8,9})\\b", Pattern.CASE_INSENSITIVE)
        val dtdcMatcher = dtdcRegex.matcher(text)
        if (dtdcMatcher.find()) {
            return dtdcMatcher.group(1)?.uppercase(Locale.US) ?: ""
        }

        // 5. Look for standalone 11-14 digit numbers (standard BlueDart / Delhivery / FedEx barcodes)
        val numericRegex = Pattern.compile("\\b(\\d{11,14})\\b")
        val numMatcher = numericRegex.matcher(text)
        if (numMatcher.find()) {
            return numMatcher.group(1) ?: ""
        }

        // 6. Look for standalone 10 digit number if it doesn't look like a phone number (doesn't start with 6-9)
        val tenDigitRegex = Pattern.compile("\\b([1-5]\\d{9})\\b")
        val tenMatcher = tenDigitRegex.matcher(text)
        if (tenMatcher.find()) {
            return tenMatcher.group(1) ?: ""
        }

        return ""
    }

    private fun extractPincode(text: String, lines: List<String>): String {
        // Look for explicitly labeled PIN / Postal code
        val labeledRegex = Pattern.compile("(?i)(?:PIN|PINCODE|PIN\\s*CODE|POSTAL|ZIP)[\\s.:#-]+([1-9][0-9]{5})\\b")
        val matcher = labeledRegex.matcher(text)
        if (matcher.find()) {
            return matcher.group(1) ?: ""
        }

        // Look for any standard 6-digit Indian PIN code
        val pinRegex = Pattern.compile("\\b([1-9][0-9]{5})\\b")
        val pinMatcher = pinRegex.matcher(text)
        val candidates = mutableListOf<String>()
        while (pinMatcher.find()) {
            val candidate = pinMatcher.group(1)
            // Filter out numbers that are parts of dates like 202610
            if (candidate != null && !candidate.startsWith("202")) {
                candidates.add(candidate)
            }
        }
        return candidates.lastOrNull() ?: candidates.firstOrNull() ?: ""
    }

    private fun extractPhone(text: String, lines: List<String>): String {
        // Check for labeled phone
        val labeledRegex = Pattern.compile(
            "(?i)(?:PH|PHONE|TEL|MOBILE|MOB|CONTACT|CELL)[\\s.:#-]+(?:\\+?91[\\s-]?)?([6-9]\\d{9})\\b"
        )
        val matcher = labeledRegex.matcher(text)
        if (matcher.find()) {
            return matcher.group(1) ?: ""
        }

        // Check for any 10-digit Indian mobile number
        val mobileRegex = Pattern.compile("\\b(?:\\+?91[\\s-]?)?([6-9]\\d{9})\\b")
        val mobMatcher = mobileRegex.matcher(text)
        if (mobMatcher.find()) {
            return mobMatcher.group(1) ?: ""
        }

        return ""
    }

    private fun extractWeight(text: String, lines: List<String>): String {
        val weightRegex = Pattern.compile(
            "(?i)(?:WT|WEIGHT|GROSS\\s*WT|NET\\s*WT)[\\s.:#-]+(\\d+(?:\\.\\d+)?\\s*(?:kg|kgs|g|gm|gms|lbs))"
        )
        val matcher = weightRegex.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }

        val standaloneWeight = Pattern.compile("\\b(\\d+(?:\\.\\d+)?\\s*(?:kg|kgs|g|gm|gms))\\b", Pattern.CASE_INSENSITIVE)
        val sMatcher = standaloneWeight.matcher(text)
        if (sMatcher.find()) {
            return sMatcher.group(1)?.trim() ?: ""
        }

        return ""
    }

    private fun extractReference(text: String, lines: List<String>): String {
        val refRegex = Pattern.compile(
            "(?i)(?:ORDER\\s*ID|ORDER\\s*NO|ORDER#|REF\\s*NO|REF#|INVOICE|INV\\s*NO|PO\\s*NO)[\\s.:#-]+([A-Z0-9-]{4,20})"
        )
        val matcher = refRegex.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim() ?: ""
        }
        return ""
    }

    private fun extractState(text: String): String {
        for (state in INDIAN_STATES) {
            val stateRegex = Pattern.compile("\\b" + Pattern.quote(state) + "\\b", Pattern.CASE_INSENSITIVE)
            if (stateRegex.matcher(text).find()) {
                return state
            }
        }
        return ""
    }

    private fun extractCity(text: String, state: String): String {
        for (city in INDIAN_CITIES) {
            val cityRegex = Pattern.compile("\\b" + Pattern.quote(city) + "\\b", Pattern.CASE_INSENSITIVE)
            if (cityRegex.matcher(text).find()) {
                return city
            }
        }
        return ""
    }

    private fun extractConsigneeName(lines: List<String>): String {
        // Look for line following "TO:" or "CONSIGNEE:" or "SHIP TO:"
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase(Locale.US)
            if (lower.startsWith("to:") || lower.startsWith("ship to:") || lower.startsWith("deliver to:") || lower.startsWith("consignee:")) {
                val direct = line.substringAfter(":").trim()
                if (direct.length > 2 && !direct.contains("address", ignoreCase = true)) {
                    return cleanName(direct)
                }
                if (i + 1 < lines.size) {
                    val next = lines[i + 1].trim()
                    if (isLikelyName(next)) return cleanName(next)
                }
            }
        }

        // Search for "Name:"
        for (line in lines) {
            if (line.contains("Name:", ignoreCase = true)) {
                val candidate = line.substringAfter("Name:", "").substringAfter("name:", "").trim()
                if (candidate.length > 2) return cleanName(candidate)
            }
        }

        // Heuristic: top lines often contain the name if not prefixed
        for (line in lines.take(6)) {
            if (isLikelyName(line) && !line.contains("courier", ignoreCase = true) && !line.contains("post", ignoreCase = true)) {
                return cleanName(line)
            }
        }

        return "Valued Customer"
    }

    private fun extractShipperName(lines: List<String>): String {
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase(Locale.US)
            if (lower.startsWith("from:") || lower.startsWith("shipper:") || lower.startsWith("return to:") || lower.startsWith("seller:")) {
                val direct = line.substringAfter(":").trim()
                if (direct.length > 2) return cleanName(direct)
                if (i + 1 < lines.size) return cleanName(lines[i + 1].trim())
            }
        }
        return ""
    }

    private fun extractAddress(lines: List<String>, consigneeName: String, city: String, pincode: String): String {
        val addressLines = mutableListOf<String>()
        var recording = false

        for (line in lines) {
            val lower = line.lowercase(Locale.US)
            if (lower.startsWith("to:") || lower.startsWith("ship to:") || lower.startsWith("deliver to:")) {
                recording = true
                continue
            }
            if (recording) {
                if (lower.startsWith("from:") || lower.startsWith("shipper:") || lower.contains("awb") || lower.contains("tracking")) {
                    break
                }
                if (!line.contains(consigneeName, ignoreCase = true) && line.length > 3) {
                    addressLines.add(line)
                    if (addressLines.size >= 3) break
                }
            }
        }

        if (addressLines.isNotEmpty()) {
            return addressLines.joinToString(", ")
        }

        // Fallback: pick lines containing typical address keywords
        val addressKeywords = listOf("street", "road", "rd", "nagar", "colony", "flat", "floor", "plot", "house", "building", "sector", "lane", "cross", "main", "layout", "opp", "near", "behind")
        val candidateLines = lines.filter { line ->
            val l = line.lowercase(Locale.US)
            addressKeywords.any { l.contains(it) } && !l.contains("awb")
        }

        if (candidateLines.isNotEmpty()) {
            return candidateLines.joinToString(", ")
        }

        return if (city.isNotEmpty()) "$city, PIN - $pincode" else "Local Delivery Address"
    }

    private fun isLikelyName(line: String): Boolean {
        if (line.length < 3 || line.length > 35) return false
        if (line.any { it.isDigit() }) return false
        val words = line.split("\\s+".toRegex())
        return words.size in 1..4 && words.all { it.isNotEmpty() && it[0].isUpperCase() }
    }

    private fun cleanName(name: String): String {
        return name.replace("(?i)^to[:\\s-]+".toRegex(), "")
            .replace("(?i)^name[:\\s-]+".toRegex(), "")
            .trim()
    }
}
