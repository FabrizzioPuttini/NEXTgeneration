package com.example.myapplication;

import static org.junit.Assert.*;

import android.view.*;
import android.widget.TextView;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ResourceListFragmentTest {
  @Test
  public void recreationDetachesOldViewAndAttachesNewAdapter() {
    AtomicReference<RecyclerView> oldView = new AtomicReference<>();
    try (FragmentScenario<ResourceListFragment> scenario =
        FragmentScenario.launchInContainer(
            ResourceListFragment.class, null, R.style.Theme_MyApplication)) {
      scenario.onFragment(fragment -> {
        RecyclerView list = fragment.requireView().findViewById(R.id.resourceRecycler);
        RecyclerView.Adapter<?> adapter = new ExampleAdapter();
        fragment.setAdapter(adapter);
        assertNotNull(list.getLayoutManager());
        assertSame(adapter, list.getAdapter());
        oldView.set(list);
      });
      scenario.recreate();
      scenario.onFragment(fragment -> {
        RecyclerView list = fragment.requireView().findViewById(R.id.resourceRecycler);
        assertNotSame(oldView.get(), list);
        assertNull(oldView.get().getAdapter());
        RecyclerView.Adapter<?> replacement = new ExampleAdapter();
        fragment.setAdapter(replacement);
        assertSame(replacement, list.getAdapter());
        assertEquals(1, list.getAdapter().getItemCount());
      });
    }
    assertNull(oldView.get().getAdapter());
  }

  public static class ShellFragment extends androidx.fragment.app.Fragment {
    public ShellFragment() {
      super(R.layout.activity_hub);
    }
  }

  @Test
  public void authenticatedShellActuallyHostsResourceFragment() {
    try (FragmentScenario<ShellFragment> scenario =
        FragmentScenario.launchInContainer(
            ShellFragment.class, null, R.style.Theme_MyApplication)) {
      scenario.onFragment(shell -> {
        androidx.fragment.app.Fragment nested =
            shell.getChildFragmentManager().findFragmentById(R.id.contentList);
        assertTrue(nested instanceof ResourceListFragment);
        RecyclerView view = nested.requireView().findViewById(R.id.resourceRecycler);
        assertNotNull(view.getLayoutManager());
        ((ResourceListFragment) nested).setAdapter(new ExampleAdapter());
        assertEquals(1, view.getAdapter().getItemCount());
      });
      scenario.recreate();
      scenario.onFragment(shell -> {
        assertTrue(
            shell.getChildFragmentManager().findFragmentById(R.id.contentList)
                instanceof ResourceListFragment);
      });
    }
  }

  private static final class ExampleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
      return new RecyclerView.ViewHolder(new TextView(parent.getContext())) {};
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      ((TextView) holder.itemView).setText("Teste de lista");
    }

    @Override
    public int getItemCount() {
      return 1;
    }
  }
}
