package com.example.projectkp.model.surat

data class Surat(
    val id: Int,
    val judul: String,
    val tujuan: String,
    val tanggal_mulai: String,
    val tanggal_berakhir: String,
    val file_pdf: String
)