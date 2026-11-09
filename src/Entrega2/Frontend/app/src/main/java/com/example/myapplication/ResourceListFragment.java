package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Owns the list view lifecycle; the host supplies the current screen's adapter. */
public class ResourceListFragment extends Fragment {
  public interface AdapterHost {
    RecyclerView.Adapter<?> resourceAdapter();
    default void refreshResources() {}
    default boolean resourcesLoading() { return false; }
  }

  private RecyclerView list;
  private androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh;

  public ResourceListFragment() {
    super(R.layout.fragment_resource_list);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    list = view.findViewById(R.id.resourceRecycler);
    refresh=view.findViewById(R.id.refreshLayout);
    refresh.setOnChildScrollUpCallback((parent, child) -> list != null && list.canScrollVertically(-1));
    refresh.setOnRefreshListener(() -> {
      if(requireActivity() instanceof AdapterHost) ((AdapterHost)requireActivity()).refreshResources();
      else refresh.setRefreshing(false);
    });
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    if (requireActivity() instanceof AdapterHost)
      setAdapter(((AdapterHost) requireActivity()).resourceAdapter());
  }

  public void setRefreshing(boolean loading) {
    if(refresh != null) refresh.setRefreshing(loading);
  }
  @Override public void onStart() {
    super.onStart();
    if(requireActivity() instanceof AdapterHost) setRefreshing(((AdapterHost)requireActivity()).resourcesLoading());
  }
  @Override public void onStop() { setRefreshing(false); super.onStop(); }

  public void setAdapter(RecyclerView.Adapter<?> adapter) {
    if (list == null)
      throw new IllegalStateException("A lista deve ter uma view ativa antes de receber dados.");
    list.setAdapter(adapter);
  }

  @Override
  public void onDestroyView() {
    if (refresh != null) { refresh.setRefreshing(false); refresh.setOnRefreshListener(null); }
    refresh=null;
    if (list != null) list.setAdapter(null);
    list = null;
    super.onDestroyView();
  }
}
