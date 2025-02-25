package com.example.projectkp.model.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.projectkp.R
import com.example.projectkp.databinding.ItemSuratBinding
import com.example.projectkp.model.surat.Surat
import java.text.SimpleDateFormat
import java.util.*

class SuratAdapter(private val suratList: List<Surat>, private val onPdfClick: (String) -> Unit) :
    RecyclerView.Adapter<SuratAdapter.SuratViewHolder>() {

    class SuratViewHolder(private val binding: ItemSuratBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(surat: Surat, onPdfClick: (String) -> Unit) {
            binding.apply {
                tvJudul.text = surat.judul
                tvTujuan.text = surat.tujuan
                tvTanggal.text = "Periode: ${surat.tanggal_mulai} s/d ${surat.tanggal_berakhir}"

                // Atur warna background jika expired
                val isExpired = isExpired(surat.tanggal_berakhir)
                cdSurat.setBackgroundColor(
                    cdSurat.context.getColor(if (isExpired) R.color.gray_medium else R.color.white)
                )

                btnViewPdf.setOnClickListener {
                    onPdfClick(surat.file_pdf)
                }
            }
        }

        // Fungsi untuk cek expired (boleh dihapus kalau sudah dicek di Activity)
        private fun isExpired(tanggalBerakhir: String): Boolean {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val expiryDate = sdf.parse(tanggalBerakhir)
                val today = Date()
                expiryDate != null && expiryDate.before(today)
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuratViewHolder {
        val binding = ItemSuratBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SuratViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuratViewHolder, position: Int) {
        holder.bind(suratList[position], onPdfClick)
    }

    override fun getItemCount() = suratList.size
}
