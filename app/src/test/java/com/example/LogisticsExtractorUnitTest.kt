package com.example

import com.example.ocr.LogisticsDataExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogisticsExtractorUnitTest {

    @Test
    fun testDelhiveryLabelExtraction() {
        val sampleOcr = """
            DELHIVERY SURFACE
            AWB: 14029482019482
            To: Ramesh Gupta
            Flat 204, Green Heights, Outer Ring Road
            Bengaluru, Karnataka - 560103
            Mob: 9845123456
            Order ID: ORD-99201
            Weight: 1.5 kg
        """.trimIndent()

        val extracted = LogisticsDataExtractor.extract(sampleOcr, 1)

        assertEquals("Delhivery", extracted.courierPartner)
        assertEquals("14029482019482", extracted.awbNumber)
        assertEquals("560103", extracted.pincode)
        assertEquals("9845123456", extracted.consigneePhone)
        assertEquals("ORD-99201", extracted.referenceNumber)
        assertEquals("Bengaluru", extracted.city)
        assertEquals("Karnataka", extracted.state)
        assertTrue(extracted.weight.contains("1.5"))
    }

    @Test
    fun testBlueDartLabelExtraction() {
        val sampleOcr = """
            BLUE DART COURIER
            WAYBILL: 84920194820
            Consignee: Ananya Sen
            Plot 12, Park Street
            Kolkata, West Bengal 700016
            Phone: 9830123456
            Wt: 0.8 kg
        """.trimIndent()

        val extracted = LogisticsDataExtractor.extract(sampleOcr, 2)

        assertEquals("Blue Dart", extracted.courierPartner)
        assertEquals("84920194820", extracted.awbNumber)
        assertEquals("700016", extracted.pincode)
        assertEquals("9830123456", extracted.consigneePhone)
        assertEquals("Kolkata", extracted.city)
        assertEquals("West Bengal", extracted.state)
    }

    @Test
    fun testSpeedPostExtraction() {
        val sampleOcr = """
            SPEED POST (INDIA POST)
            Article No: EM948201948IN
            To: Dr. Sandeep Kulkarni
            Deccan Gymkhana
            Pune, Maharashtra 411004
            Ph: 9422012345
        """.trimIndent()

        val extracted = LogisticsDataExtractor.extract(sampleOcr, 3)

        assertEquals("India Post / Speed Post", extracted.courierPartner)
        assertEquals("EM948201948IN", extracted.awbNumber)
        assertEquals("411004", extracted.pincode)
        assertEquals("Pune", extracted.city)
        assertEquals("Maharashtra", extracted.state)
    }
}
