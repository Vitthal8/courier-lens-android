package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.ShipmentEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ManifestExportService {

    enum class ExportFormat {
        EXCEL_XLSX,
        CSV
    }

    data class ExportResult(
        val file: File,
        val uri: Uri,
        val format: ExportFormat,
        val recordCount: Int
    )

    fun exportShipments(
        context: Context,
        shipments: List<ShipmentEntity>,
        format: ExportFormat
    ): ExportResult {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val (file, mimeType) = when (format) {
            ExportFormat.EXCEL_XLSX -> {
                val f = File(exportDir, "Courier_Manifest_$timestamp.xlsx")
                writeXlsx(f, shipments)
                f to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            }
            ExportFormat.CSV -> {
                val f = File(exportDir, "Courier_Manifest_$timestamp.csv")
                writeCsv(f, shipments)
                f to "text/csv"
            }
        }

        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        return ExportResult(
            file = file,
            uri = uri,
            format = format,
            recordCount = shipments.size
        )
    }

    fun shareExportFile(context: Context, exportResult: ExportResult) {
        val mimeType = when (exportResult.format) {
            ExportFormat.EXCEL_XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ExportFormat.CSV -> "text/csv"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, exportResult.uri)
            putExtra(Intent.EXTRA_SUBJECT, "Courier Shipment Manifest (${exportResult.recordCount} items)")
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached is the CourierLens shipment manifest export containing ${exportResult.recordCount} items."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share Courier Manifest via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun writeCsv(file: File, shipments: List<ShipmentEntity>) {
        FileOutputStream(file).use { fos ->
            // Write UTF-8 BOM so Excel opens with proper encoding
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            val headers = listOf(
                "Serial No", "AWB Number", "Courier Partner", "Consignee Name",
                "Phone", "Address", "City", "State", "PIN Code", "Country",
                "Shipper Name", "Reference ID", "Weight", "Status", "Scanned Date"
            )

            fos.write(headers.joinToString(",") { escapeCsv(it) }.toByteArray(Charsets.UTF_8))
            fos.write("\n".toByteArray(Charsets.UTF_8))

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

            for (s in shipments) {
                val row = listOf(
                    s.serialNumber,
                    s.awbNumber,
                    s.courierPartner,
                    s.consigneeName,
                    s.consigneePhone,
                    s.addressLine,
                    s.city,
                    s.state,
                    s.pincode,
                    s.country,
                    s.shipperName,
                    s.referenceNumber,
                    s.weight,
                    s.status,
                    dateFormat.format(Date(s.createdAt))
                )
                fos.write(row.joinToString(",") { escapeCsv(it) }.toByteArray(Charsets.UTF_8))
                fos.write("\n".toByteArray(Charsets.UTF_8))
            }
        }
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Builds a valid Microsoft Excel .xlsx OpenXML package using standard ZipOutputStream.
     */
    private fun writeXlsx(file: File, shipments: List<ShipmentEntity>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sheetDataXml = StringBuilder()
        // Header Row (Row 1)
        val headers = listOf(
            "Serial No", "AWB Tracking No", "Courier Partner", "Consignee Name",
            "Phone Number", "Delivery Address", "City", "State", "PIN Code",
            "Country", "Shipper", "Reference / Order ID", "Weight", "Status", "Date Scanned"
        )

        sheetDataXml.append("<row r=\"1\">")
        headers.forEachIndexed { colIdx, header ->
            val cellRef = getColumnName(colIdx) + "1"
            sheetDataXml.append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t>${xmlEscape(header)}</t></is></c>")
        }
        sheetDataXml.append("</row>")

        // Data Rows
        shipments.forEachIndexed { rowIdx, s ->
            val r = rowIdx + 2
            val values = listOf(
                s.serialNumber,
                s.awbNumber,
                s.courierPartner,
                s.consigneeName,
                s.consigneePhone,
                s.addressLine,
                s.city,
                s.state,
                s.pincode,
                s.country,
                s.shipperName,
                s.referenceNumber,
                s.weight,
                s.status,
                dateFormat.format(Date(s.createdAt))
            )

            sheetDataXml.append("<row r=\"$r\">")
            values.forEachIndexed { colIdx, v ->
                val cellRef = getColumnName(colIdx) + r
                sheetDataXml.append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t>${xmlEscape(v)}</t></is></c>")
            }
            sheetDataXml.append("</row>")
        }

        val sheet1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetData>
        $sheetDataXml
    </sheetData>
</worksheet>"""

        val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="Courier Manifest" sheetId="1" r:id="rId1"/>
    </sheets>
</workbook>"""

        val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            fun addEntry(path: String, content: String) {
                zos.putNextEntry(ZipEntry(path))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }

            addEntry("[Content_Types].xml", contentTypesXml)
            addEntry("_rels/.rels", rootRelsXml)
            addEntry("xl/workbook.xml", workbookXml)
            addEntry("xl/_rels/workbook.xml.rels", workbookRelsXml)
            addEntry("xl/worksheets/sheet1.xml", sheet1Xml)
        }
    }

    private fun getColumnName(index: Int): String {
        val sb = StringBuilder()
        var n = index
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun xmlEscape(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
