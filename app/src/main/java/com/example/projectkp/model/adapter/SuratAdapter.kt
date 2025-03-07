package com.example.projectkp.model.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.projectkp.R
import com.example.projectkp.databinding.ItemSuratBinding
import com.example.projectkp.model.surat.Surat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SuratAdapter(private val suratList: List<Surat>, private val onPdfClick: (String) -> Unit) :
    RecyclerView.Adapter<SuratAdapter.SuratViewHolder>() {

    class SuratViewHolder(private val binding: ItemSuratBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(surat: Surat, onPdfClick: (String) -> Unit) {
            binding.apply {
                tvJudul.text = surat.judul
                tvTujuan.text = surat.tujuan
                // Format tanggal ke format Tanggal Bulan Tahun
                val tanggalMulaiFormatted = formatDate(surat.tanggal_mulai)
                val tanggalBerakhirFormatted = formatDate(surat.tanggal_berakhir)
                tvTanggal.text = "Periode: ${tanggalMulaiFormatted} s/d ${tanggalBerakhirFormatted}"

                // Atur warna background jika expired
                val isExpired = isExpired(surat.tanggal_berakhir)
                cdSurat.setBackgroundColor(
                    cdSurat.context.getColor(if (isExpired) R.color.gray_medium else R.color.white)
                )

                btnViewPdf.setOnClickListener {
                    onPdfClick(surat.id.toString())
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

        private fun formatDate(dateString: String): String {
            try {
                // Assuming input date is in format like "yyyy-MM-dd" or similar
                // Adjust the input format according to your actual date format
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale("id"))
                val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id"))

                val date = inputFormat.parse(dateString)
                return outputFormat.format(date)
            } catch (e: Exception) {
                // Return original string if parsing fails
                return dateString
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
