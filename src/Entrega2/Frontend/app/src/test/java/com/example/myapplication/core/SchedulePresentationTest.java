package com.example.myapplication.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class SchedulePresentationTest {
 @Test public void instantUsesSaoPauloIncludingPreviousDay() {
  assertEquals("08/11/2026 · 21h40",SchedulePresentation.courseDate("2026-11-09T00:40:37.542Z"));
  assertEquals("08/11/2026 · 21h40",SchedulePresentation.courseDate("2026-11-08T21:40:37-03:00"));
 }
 @Test public void postgresFractionAndDateOnlyAreSupported() {
  assertEquals("08/11/2026 · 21h40",SchedulePresentation.courseDate("2026-11-09T00:40:37.542123+00:00"));
  assertEquals("08/11/2026 · 21h40",SchedulePresentation.courseDate("2026-11-09T00:40:37.123456789Z"));
  assertEquals("09/11/2026",SchedulePresentation.courseDate("2026-11-09"));
 }
 @Test public void agendaDateAndTimeAreAlreadyLocal() {
  assertEquals("09/11/2026",SchedulePresentation.agendaDate("2026-11-09"));
  assertEquals("09",SchedulePresentation.day("2026-11-09"));
  assertEquals("NOV",SchedulePresentation.month("2026-11-09"));
  assertEquals("08h30–10h00",SchedulePresentation.timeRange("08:30:00","10:00:00"));
  assertEquals("08h30",SchedulePresentation.timeRange("08:30:00",null));
 }
 @Test public void malformedDatesAndTimesHaveFriendlyFallbacks() {
  for(String invalid:new String[]{null,"","texto","2026-02-30","2026-11-09T99:00:00Z"})
   assertEquals("Data a definir",SchedulePresentation.courseDate(invalid));
  assertEquals("Data a definir",SchedulePresentation.agendaDate("2026-02-30"));
  assertEquals("Horário a definir",SchedulePresentation.timeRange("25:05:00",""));
  assertEquals("08h30",SchedulePresentation.timeRange("08:30","x"));
 }
 @Test public void metadataUsesSingularAndDoesNotInventMissingValues() {
  assertEquals("1 encontro",SchedulePresentation.meetings("1"));
  assertEquals("2 encontros",SchedulePresentation.meetings("2"));
  assertEquals("Encontros a definir",SchedulePresentation.meetings(""));
  assertEquals("8h de carga horária",SchedulePresentation.workload("8"));
  assertEquals("Carga horária a definir",SchedulePresentation.workload("null"));
 }
}
