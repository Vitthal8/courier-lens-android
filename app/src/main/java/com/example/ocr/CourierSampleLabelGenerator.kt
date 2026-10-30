package com.example.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CourierSampleLabelGenerator {

    fun generateSampleBitmap(index: Int = 1): Bitmap {
        val width = 720
        val height = 960
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // White shipping label background
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(16f, 16f, width - 16f, height - 16f, borderPaint)

        // Header section with courier brand
        val couriers = listOf(
            Triple("DELHIVERY SURFACE", "DEL94820194820", "Bengaluru"),
            Triple("BLUE DART EXPRESS", "85412948201", "Mumbai"),
            Triple("DTDC PLUS", "D94820148", "Noida"),
            Triple("SPEED POST", "EM849201948IN", "Chennai")
        )
        val selected = couriers[(index - 1) % couriers.size]

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(selected.first, 40f, 75f, titlePaint)

        // Horizontal divider
        canvas.drawLine(16f, 100f, width - 16f, 100f, borderPaint)

        // Barcode Simulation
        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        var barX = 60f
        val barcodePattern = listOf(6f, 14f, 4f, 18f, 8f, 6f, 16f, 4f, 12f, 8f, 18f, 4f, 10f, 14f, 6f, 16f, 8f, 12f, 4f, 18f, 6f, 10f, 14f, 6f, 12f, 8f)
        for (w in barcodePattern) {
            canvas.drawRect(barX, 120f, barX + w, 220f, barPaint)
            barX += w + 8f
        }

        // AWB Text under barcode
        val awbPaint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        canvas.drawText("AWB: ${selected.second}", 60f, 260f, awbPaint)

        canvas.drawLine(16f, 290f, width - 16f, 290f, borderPaint)

        // Consignee section
        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 30f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        canvas.drawText("SHIP TO / CONSIGNEE:", 40f, 335f, labelPaint)

        val names = listOf("Rahul Deshmukh", "Pooja Swaminathan", "Vikram Malhotra", "Meera Sen")
        val chosenName = names[(index - 1) % names.size]

        canvas.drawText(chosenName, 40f, 385f, titlePaint)
        canvas.drawText("Flat 402, Lotus Enclave, 10th Cross", 40f, 435f, textPaint)
        canvas.drawText("Opposite Metro Station, Indiranagar", 40f, 485f, textPaint)

        val cityLines = listOf(
            "Bengaluru, Karnataka - 560038",
            "Mumbai, Maharashtra - 400051",
            "Noida, Uttar Pradesh - 201301",
            "Chennai, Tamil Nadu - 600017"
        )
        canvas.drawText(cityLines[(index - 1) % cityLines.size], 40f, 535f, titlePaint)
        canvas.drawText("Contact: +91 98451 23456", 40f, 585f, textPaint)

        canvas.drawLine(16f, 620f, width - 16f, 620f, borderPaint)

        // Shipper & Metadata
        canvas.drawText("RETURN / SHIPPER:", 40f, 660f, labelPaint)
        canvas.drawText("E-Commerce Fulfillment Hub #4", 40f, 700f, textPaint)
        canvas.drawText("Logistics Park, Hub Road", 40f, 740f, textPaint)

        canvas.drawLine(16f, 770f, width - 16f, 770f, borderPaint)

        // Order & Weight
        canvas.drawText("ORDER ID: ORD-2026-9918", 40f, 820f, textPaint)
        canvas.drawText("WEIGHT: 0.85 kg", 40f, 870f, textPaint)
        canvas.drawText("DATE: " + SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date()), 40f, 920f, textPaint)

        return bitmap
    }
}
