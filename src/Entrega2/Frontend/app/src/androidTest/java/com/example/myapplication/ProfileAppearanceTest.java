package com.example.myapplication;
import static org.junit.Assert.*;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.Test;
public class ProfileAppearanceTest extends DemoCertificateTest {
 @Test public void profileKeepsAppearanceAndDemoOffline() {
  android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
  try(ActivityScenario<HubActivity> scenario=ActivityScenario.launch(new Intent(context,HubActivity.class).putExtra("section","perfil"))) {
   scenario.onActivity(activity->{
    RecyclerView list=activity.findViewById(R.id.resourceRecycler);
    RecyclerView.Adapter adapter=list.getAdapter();
    StringBuilder text=new StringBuilder();
    for(int i=0;i<adapter.getItemCount();i++) {
     RecyclerView.ViewHolder holder=adapter.createViewHolder(list,adapter.getItemViewType(i));
     adapter.bindViewHolder(holder,i);
     text.append(((android.widget.TextView)holder.itemView.findViewById(R.id.rowTitle)).getText());
    }
    assertTrue(text.toString(),text.toString().contains("Aparência"));
    assertTrue(text.toString(),text.toString().contains("Certificado de demonstração"));
   });
  }
 }
 @Test public void themePreferencePersistsAndRejectsUnknownModes() {
  android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
  int previous=ThemePreference.get(context);
  try {
   for(int mode:new int[]{-1,1,2}) {
    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->ThemePreference.set(context,mode));
    assertEquals(mode,ThemePreference.get(context.getApplicationContext()));
    assertEquals(mode,androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode());
   }
   assertThrows(IllegalArgumentException.class,()->ThemePreference.set(context,77));
   assertEquals(2,ThemePreference.get(context));
  } finally {
   androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(()->ThemePreference.set(context,previous));
  }
 }
 @Test public void failedRefreshStopsSpinnerAndNavigationCanLoadAgain() throws Exception {
  android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
  try(ActivityScenario<HubActivity> scenario=ActivityScenario.launch(new Intent(context,HubActivity.class).putExtra("section","perfil"))) {
   long deadline=System.currentTimeMillis()+15000;
   java.util.concurrent.atomic.AtomicBoolean loading=new java.util.concurrent.atomic.AtomicBoolean(true);
   while(loading.get()&&System.currentTimeMillis()<deadline) {
    scenario.onActivity(a->loading.set(a.resourcesLoading()));
    Thread.sleep(100);
   }
   scenario.onActivity(a->{
    assertFalse(a.resourcesLoading());
    androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=a.findViewById(R.id.refreshLayout);
    assertFalse(refresh.isRefreshing());
    a.refreshResources();
    assertTrue(a.resourcesLoading());
    a.refreshResources();
    a.findViewById(R.id.navCursos).performClick();
   });
   scenario.recreate();
  }
 }
 @Test public void completedWriteSupersedesLoadingGetAndOldCompletion() throws Exception {
  android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
  java.lang.reflect.Field generation=HubActivity.class.getDeclaredField("generation");
  generation.setAccessible(true);
  java.lang.reflect.Method current=HubActivity.class.getDeclaredMethod("current",int.class);
  current.setAccessible(true);
  try(ActivityScenario<HubActivity> scenario=ActivityScenario.launch(new Intent(context,HubActivity.class).putExtra("section","perfil"))) {
   scenario.onActivity(a->{
    try {
     // All calls run in one main-thread turn, before any GET completion can paint.
     int before=generation.getInt(a);
     a.reloadAfterMutation(before);
     assertTrue(generation.getInt(a)>before);
     assertTrue(a.resourcesLoading());
     assertFalse((Boolean)current.invoke(a,before));
     int after=generation.getInt(a);
     a.reloadAfterMutation(before);
     assertEquals(after,generation.getInt(a));
    } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
   });
  }
 }
 @Test public void selectedThemeControlsConfigurationAndDisablesForcedDark() {
  android.app.Instrumentation instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
  android.content.Context context=instrumentation.getTargetContext();
  int previous=ThemePreference.get(context);
  try {
   for(int mode:new int[]{androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
       androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES}) {
    instrumentation.runOnMainSync(()->ThemePreference.set(context,mode));
    try(ActivityScenario<HubActivity> scenario=ActivityScenario.launch(new Intent(context,HubActivity.class).putExtra("section","perfil"))) {
     scenario.onActivity(a->{
      boolean dark=mode==androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
      int night=a.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
      assertEquals(dark?android.content.res.Configuration.UI_MODE_NIGHT_YES:android.content.res.Configuration.UI_MODE_NIGHT_NO,night);
      int expected=android.graphics.Color.parseColor(dark?"#101917":"#FBFBFB");
      assertEquals(expected,((android.graphics.drawable.ColorDrawable)a.findViewById(R.id.screenRoot).getBackground()).getColor());
      if(android.os.Build.VERSION.SDK_INT>=29) {
       android.util.TypedValue forceDark=new android.util.TypedValue();
       assertTrue("Theme must explicitly declare its force-dark policy",a.getTheme().resolveAttribute(android.R.attr.forceDarkAllowed,forceDark,true));
       assertEquals("Manufacturer forced dark must not override the selected theme",0,forceDark.data);
      }
     });
    }
   }
  } finally { instrumentation.runOnMainSync(()->ThemePreference.set(context,previous)); }
 }
 @Test public void profileMenuOpensFromWholeRow() {
  android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
  try(ActivityScenario<HubActivity> scenario=ActivityScenario.launch(new Intent(context,HubActivity.class).putExtra("section","perfil"))) {
   scenario.onActivity(a->{
    RecyclerView list=a.findViewById(R.id.resourceRecycler);
    RecyclerView.Adapter adapter=list.getAdapter();
    boolean found=false;
    for(int i=0;i<adapter.getItemCount();i++) {
     RecyclerView.ViewHolder holder=adapter.createViewHolder(list,adapter.getItemViewType(i));
     adapter.bindViewHolder(holder,i);
     if("Minha carteirinha".contentEquals(((android.widget.TextView)holder.itemView.findViewById(R.id.rowTitle)).getText())) {
      found=true;
      assertTrue("Menu row must be an actionable touch target",holder.itemView.performClick());
      assertEquals("Carteirinha",((android.widget.TextView)a.findViewById(R.id.screenTitle)).getText().toString());
      break;
     }
    }
    assertTrue("Carteirinha remains available offline",found);
   });
  }
 }
 @Test public void profileRowsFitNarrowWidthWithLargeText() {
  android.app.Instrumentation instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
  instrumentation.runOnMainSync(()->{
   android.content.Context base=instrumentation.getTargetContext();
   android.content.res.Configuration configuration=new android.content.res.Configuration(base.getResources().getConfiguration());
   configuration.fontScale=1.5f;
   android.content.Context themed=new androidx.appcompat.view.ContextThemeWrapper(base.createConfigurationContext(configuration),R.style.Theme_MyApplication);
   for(int layout:new int[]{R.layout.item_profile_identity,R.layout.item_menu,R.layout.item_course,R.layout.item_agenda}) {
    android.view.View root=android.view.LayoutInflater.from(themed).inflate(layout,null);
    ((android.widget.TextView)root.findViewById(R.id.rowTitle)).setText("Nome completo bastante longo para validar a identificação do aluno");
    ((android.widget.TextView)root.findViewById(R.id.rowDetail)).setText("aluno.com.nome.completo.extenso@example.test");
    android.widget.TextView school=root.findViewById(R.id.profileSchool);
    if(school!=null) school.setText("Escola de Ensino Médio com Nome Extenso de Demonstração");
    for(int metadataId:new int[]{R.id.rowStatus,R.id.rowLocation,R.id.rowDate,R.id.rowWorkload,R.id.rowMeetings,R.id.rowTime}) {
     android.widget.TextView metadata=root.findViewById(metadataId);
     if(metadata!=null)metadata.setText("Informação com texto extenso para validar a leitura completa sem corte");
    }
    if(root.findViewById(R.id.rowDay)!=null) {
     ((android.widget.TextView)root.findViewById(R.id.rowDay)).setText("09");
     ((android.widget.TextView)root.findViewById(R.id.rowMonth)).setText("A DEFINIR");
    }
    int width=(int)(288*themed.getResources().getDisplayMetrics().density);
    root.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
    root.layout(0,0,width,root.getMeasuredHeight());
    for(int id:new int[]{R.id.rowTitle,R.id.rowDetail,R.id.profileSchool,R.id.rowStatus,R.id.rowLocation,R.id.rowDate,R.id.rowWorkload,R.id.rowMeetings,R.id.rowTime,R.id.rowDay,R.id.rowMonth}) {
     android.widget.TextView text=root.findViewById(id);
     if(text==null)continue;
     assertTrue("Text must grow down within its parent",text.getTop()>=0);
     assertTrue(text.getBottom()<=((android.view.View)text.getParent()).getHeight());
     assertTrue("All text lines must fit",text.getLayout().getLineBottom(text.getLineCount()-1)<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
    }
    assertTrue(root.getHeight()>=48*themed.getResources().getDisplayMetrics().density);
   }
  });
 }
 @Test public void courseAndAgendaRebindingPreservesActionsAndStatus() throws Exception {
  android.content.Context context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
  java.lang.reflect.Method render=HubActivity.class.getDeclaredMethod("render",com.google.gson.JsonElement.class,java.util.List.class);
  render.setAccessible(true);
  try(ActivityScenario<HubActivity> scenario=ActivityScenario.launch(new Intent(context,HubActivity.class).putExtra("section","cursos"))) {
   scenario.onActivity(a->{
    try {
     com.google.gson.JsonArray courses=com.google.gson.JsonParser.parseString("[{\"id\":\"one\",\"title\":\"Curso A\",\"status\":\"Disponível\",\"workload\":8,\"meetings_count\":1},{\"id\":\"two\",\"title\":\"Curso B\",\"status\":\"Inscrito\"}]").getAsJsonArray();
     render.invoke(a,courses,java.util.Collections.emptyList());
     RecyclerView list=a.findViewById(R.id.resourceRecycler);
     RecyclerView.Adapter adapter=list.getAdapter();
     RecyclerView.ViewHolder holder=adapter.createViewHolder(list,adapter.getItemViewType(0));
     adapter.bindViewHolder(holder,0);
     assertEquals("Inscrever-me",((android.widget.Button)holder.itemView.findViewById(R.id.rowAction)).getText().toString());
     adapter.bindViewHolder(holder,1);
     assertEquals("Inscrito",((android.widget.TextView)holder.itemView.findViewById(R.id.rowStatus)).getText().toString());
     holder.itemView.findViewById(R.id.rowAction).performClick();
     assertEquals("Agenda",((android.widget.TextView)a.findViewById(R.id.screenTitle)).getText().toString());
     com.google.gson.JsonArray meetings=com.google.gson.JsonParser.parseString("[{\"id\":\"one\",\"title\":\"Aula A\",\"activity_date\":\"2026-11-09\",\"start_time\":\"08:30:00\",\"attended\":true},{\"id\":\"two\",\"title\":\"Aula B\",\"activity_date\":\"2026-11-10\",\"attended\":false}]").getAsJsonArray();
     render.invoke(a,meetings,java.util.Collections.emptyList());
     holder=adapter.createViewHolder(list,adapter.getItemViewType(0));
     adapter.bindViewHolder(holder,0);
     assertEquals("08h30",((android.widget.TextView)holder.itemView.findViewById(R.id.rowTime)).getText().toString());
     adapter.bindViewHolder(holder,1);
     assertEquals("Presença não registrada",((android.widget.TextView)holder.itemView.findViewById(R.id.rowStatus)).getText().toString());
     assertEquals("10",((android.widget.TextView)holder.itemView.findViewById(R.id.rowDay)).getText().toString());
     holder.itemView.findViewById(R.id.rowAction).performClick();
     assertEquals("Presenças",((android.widget.TextView)a.findViewById(R.id.screenTitle)).getText().toString());
    } catch(ReflectiveOperationException e) { throw new AssertionError(e); }
   });
  }
 }
}
