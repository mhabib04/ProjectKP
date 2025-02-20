package com.example.projectkp.api

import com.example.projectkp.model.surat.Surat
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @Multipart
    @POST("surat")
    fun insertSurat(
        @Part("judul") judul: RequestBody,
        @Part("tujuan") tujuan: RequestBody,
        @Part("tanggal_mulai") tanggalMulai: RequestBody,
        @Part("tanggal_berakhir") tanggalBerakhir: RequestBody,
        @Part file_pdf: MultipartBody.Part
    ): Call<Void>

    @GET("surats")
    fun getSuratList(): Call<List<Surat>>

    @GET("surats/{id}")
    fun getSurat(
        @Path("id") id: Int
    ): Call<Surat>

    @GET("surats/{id}/pdf")
    @Streaming
    fun getPdf(
        @Path("id") id: Int
    ): Call<ResponseBody>
}