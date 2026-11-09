package com.example.myapplication.core;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Formats API instants separately from agenda fields that already represent local time. */
public final class SchedulePresentation {
  private static final Locale PT_BR = new Locale("pt", "BR");
  private static final TimeZone SAO_PAULO = TimeZone.getTimeZone("America/Sao_Paulo");
  private SchedulePresentation() {}

  private static Date parse(String value, String pattern) {
    if (value == null) return null;
    SimpleDateFormat format = new SimpleDateFormat(pattern, PT_BR);
    format.setTimeZone(SAO_PAULO);
    format.setLenient(false);
    ParsePosition position = new ParsePosition(0);
    Date date = format.parse(value, position);
    return date != null && position.getIndex() == value.length() ? date : null;
  }

  private static String format(Date date, String pattern) {
    SimpleDateFormat format = new SimpleDateFormat(pattern, PT_BR);
    format.setTimeZone(SAO_PAULO);
    return format.format(date);
  }

  private static Date localDate(String value) {
    return value != null && value.matches("\\d{4}-\\d{2}-\\d{2}") ? parse(value,"yyyy-MM-dd") : null;
  }

  public static String courseDate(String value) {
    Date date = localDate(value);
    if (date != null) return format(date,"dd/MM/yyyy");
    if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?(Z|[+-]\\d{2}:\\d{2})")) return "Data a definir";
    if(value.contains(".")) {
      int dot=value.indexOf('.');
      int end=dot+1;
      while(end<value.length() && Character.isDigit(value.charAt(end))) end++;
      String milliseconds=(value.substring(dot+1,end)+"000").substring(0,3);
      value=value.substring(0,dot+1)+milliseconds+value.substring(end);
    }
    date = parse(value, value.contains(".") ? "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" : "yyyy-MM-dd'T'HH:mm:ssXXX");
    return date == null ? "Data a definir" : format(date,"dd/MM/yyyy · HH'h'mm");
  }

  public static String agendaDate(String value) {
    Date date=localDate(value);
    return date==null?"Data a definir":format(date,"dd/MM/yyyy");
  }
  public static String day(String value) {
    Date date=localDate(value);
    return date==null?"—":format(date,"dd");
  }
  public static String month(String value) {
    Date date=localDate(value);
    return date==null?"A DEFINIR":format(date,"MMM").replace(".","").toUpperCase(PT_BR);
  }
  private static String time(String value) {
    if (value==null || !value.matches("\\d{2}:\\d{2}(:\\d{2})?")) return null;
    Date time=parse(value,value.length()==5?"HH:mm":"HH:mm:ss");
    return time==null?null:format(time,"HH'h'mm");
  }
  public static String timeRange(String start,String end) {
    String first=time(start),last=time(end);
    return first==null?"Horário a definir":last==null?first:first+"–"+last;
  }
  public static String meetings(String count) {
    if(count==null || !count.matches("\\d+"))return "Encontros a definir";
    try { int number=Integer.parseInt(count); return number>0?number+(number==1?" encontro":" encontros"):"Encontros a definir"; }
    catch(NumberFormatException invalid){ return "Encontros a definir"; }
  }
  public static String workload(String hours) {
    if(hours==null || !hours.matches("\\d+(\\.\\d+)?"))return "Carga horária a definir";
    try { return Double.parseDouble(hours)>0?hours.replace('.',',')+"h de carga horária":"Carga horária a definir"; }
    catch(NumberFormatException invalid){ return "Carga horária a definir"; }
  }
}
