package com.example.projectkp.model.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.projectkp.databinding.ItemSuratBinding
import com.example.projectkp.model.surat.Surat

class SuratAdapter(private val suratList: List<Surat>, private val onPdfClick: (String) -> Unit) :
    RecyclerView.Adapter<SuratAdapter.SuratViewHolder>() {

    class SuratViewHolder(private val binding: ItemSuratBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(surat: Surat, onPdfClick: (String) -> Unit) {
            binding.apply {
                tvJudul.text = surat.judul
                tvTujuan.text = surat.tujuan
                tvTanggal.text = "Periode: ${surat.tanggal_mulai} s/d ${surat.tanggal_berakhir}"

                btnViewPdf.setOnClickListener {
                    onPdfClick(surat.file_pdf)
                }
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