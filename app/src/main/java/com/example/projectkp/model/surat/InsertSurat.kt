package com.example.projectkp.model.surat

import okhttp3.MultipartBody
import okhttp3.RequestBody

data class InsertSurat(
    val judul: RequestBody,
    val tujuan: RequestBody,
    val tanggal_mulai: RequestBody,
    val tanggal_berakhir: RequestBody,
    val file_pdf: MultipartBody.Part
)
