package br.com.osconecta;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ProtocolBackupWorker extends Worker {
    public static final String WORK_NAME = "osconecta-monthly-protocol-backup";
    private static final String PREFS = "osconecta_protocols";
    public ProtocolBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            JSONArray records = new JSONArray(prefs.getString("records", "[]"));
            if (records.length() == 0) return Result.success();
            Calendar today = Calendar.getInstance();
            String current = monthKey(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1);
            String last = prefs.getString("last_backup_month", "");
            String target;
            if (last.isEmpty()) {
                if (today.get(Calendar.DAY_OF_MONTH) < dueDay(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1)) return Result.success();
                target = current;
            } else {
                target = nextMonth(last);
                if (target.compareTo(current) > 0) return Result.success();
                if (target.equals(current) && today.get(Calendar.DAY_OF_MONTH) < dueDay(today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1)) return Result.success();
            }
            String name = "OSConecta-Protocolos-" + target + ".xlsx";
            if (!save(context, name, workbook(records))) return Result.retry();
            prefs.edit().putString("last_backup_month", target).apply();
            return Result.success();
        } catch (Exception e) { return Result.retry(); }
    }
    private static int dueDay(int y,int m){Calendar c=Calendar.getInstance();c.clear();c.set(y,m-1,1);return Math.min(30,c.getActualMaximum(Calendar.DAY_OF_MONTH));}
    private static String monthKey(int y,int m){return String.format(Locale.US,"%04d-%02d",y,m);}
    private static String nextMonth(String key){try{String[] p=key.split("-");Calendar c=Calendar.getInstance();c.clear();c.set(Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,1);c.add(Calendar.MONTH,1);return monthKey(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1);}catch(Exception e){Calendar c=Calendar.getInstance();return monthKey(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1);}}
    private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    private static String cell(String ref,String val){return "<c r=\""+ref+"\" t=\"inlineStr\"><is><t>"+esc(val)+"</t></is></c>";}
    private static byte[] workbook(JSONArray records) throws Exception {
        StringBuilder sheet=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        String[] heads={"Protocolo","Dia","Mês","Ano"};sheet.append("<row r=\"1\">");for(int i=0;i<4;i++)sheet.append(cell((char)('A'+i)+"1",heads[i]));sheet.append("</row>");
        int rowNo=2;for(int i=0;i<records.length();i++){JSONObject r=records.optJSONObject(i);if(r==null)continue;String number=r.optString("number","").replaceAll("\\D","");if(number.isEmpty())continue;sheet.append("<row r=\"").append(rowNo).append("\">").append(cell("A"+rowNo,number)).append(cell("B"+rowNo,String.valueOf(r.optInt("day")))).append(cell("C"+rowNo,String.valueOf(r.optInt("month")))).append(cell("D"+rowNo,String.valueOf(r.optInt("year")))).append("</row>");rowNo++;}
        sheet.append("</sheetData></worksheet>");String[][] entries={
            {"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>"},
            {"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>"},
            {"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Protocolos\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"},
            {"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"},
            {"xl/worksheets/sheet1.xml",sheet.toString()}};
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes,StandardCharsets.UTF_8)){for(String[] e:entries){zip.putNextEntry(new ZipEntry(e[0]));zip.write(e[1].getBytes(StandardCharsets.UTF_8));zip.closeEntry();}}return bytes.toByteArray();
    }
    private static boolean save(Context c,String name,byte[] bytes)throws Exception{
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,name);v.put(MediaStore.MediaColumns.MIME_TYPE,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/OS Conecta");Uri uri=c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(uri==null)return false;try(OutputStream out=c.getContentResolver().openOutputStream(uri)){if(out==null)return false;out.write(bytes);out.flush();return true;}}
        File dir=c.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);if(dir==null)dir=c.getFilesDir();if(!dir.exists())dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name))){out.write(bytes);out.flush();return true;}
    }
}
