package unc.edu.pe.cyclo.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

import unc.edu.pe.cyclo.R;
import unc.edu.pe.cyclo.databinding.ItemRecompensaBinding;
import unc.edu.pe.cyclo.model.Recompensa;

public class RecompensaAdapter extends RecyclerView.Adapter<RecompensaAdapter.ViewHolder> {

    private List<Recompensa> lista = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onCanjearClick(Recompensa recompensa);
    }

    public RecompensaAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setRecompensas(List<Recompensa> recompensas) {
        this.lista = recompensas;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecompensaBinding binding = ItemRecompensaBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Recompensa recompensa = lista.get(position);
        holder.binding.tvTitulo.setText(recompensa.getTitulo());
        holder.binding.tvDescripcion.setText(recompensa.getDescripcion());
        holder.binding.tvCostoPuntos.setText(recompensa.getCostoPuntos() + " pts");
        Glide.with(holder.itemView.getContext())
                .load(recompensa.getImagenUrl())
                .placeholder(R.drawable.img_fondo2)
                .into(holder.binding.imgRecompensa);
        holder.binding.btnCanjear.setOnClickListener(v -> listener.onCanjearClick(recompensa));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemRecompensaBinding binding;
        public ViewHolder(ItemRecompensaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}