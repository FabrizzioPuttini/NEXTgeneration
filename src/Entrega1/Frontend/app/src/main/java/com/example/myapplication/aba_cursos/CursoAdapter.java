package com.example.myapplication.aba_cursos;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import java.util.List;

public class CursoAdapter extends RecyclerView.Adapter<CursoAdapter.CursoViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Curso curso, int position);
    }

    private final List<Curso> listaCursos;
    private final OnItemClickListener listener;

    public CursoAdapter(List<Curso> listaCursos, OnItemClickListener listener) {
        this.listaCursos = listaCursos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CursoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_curso, parent, false);
        return new CursoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CursoViewHolder holder, int position) {
        Curso curso = listaCursos.get(position);
        holder.tvNomeCurso.setText(curso.getNome());
        holder.tvCargaHoraria.setText("Carga Horária: " + curso.getCargaHoraria() + "h");
        holder.tvStatus.setText(curso.getStatus());

        // Cores de status mais simples
        if ("Inscrito".equalsIgnoreCase(curso.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#1B5E20"));
        } else if ("Disponível".equalsIgnoreCase(curso.getStatus())) {
            holder.tvStatus.setTextColor(Color.parseColor("#00897B"));
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#757575"));
        }

        // Clique no card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(curso, position);
        });
    }

    @Override
    public int getItemCount() {
        return listaCursos != null ? listaCursos.size() : 0;
    }

    public static class CursoViewHolder extends RecyclerView.ViewHolder {
        public TextView tvNomeCurso, tvCargaHoraria, tvStatus;

        public CursoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNomeCurso = itemView.findViewById(R.id.tvNomeCurso);
            tvCargaHoraria = itemView.findViewById(R.id.tvCargaHoraria);
            tvStatus = itemView.findViewById(R.id.tvStatusCurso);
        }
    }
}