package com.example.myapplication.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExportUtil {
    private static final int PAGE_W = 595, PAGE_H = 842, MARGIN = 40, ROW_H = 24;

    private static File exportsDir(Activity a) {
        File dir = new File(a.getCacheDir(), "exports");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static String safe(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    // ---------------- CSV ----------------
    public static void exportCsv(Activity a, String fileName, String[] header, List<String[]> rows) {
        try {
            File f = new File(exportsDir(a), safe(fileName));
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
                w.write('\uFEFF'); // BOM so Excel reads UTF-8 (Bangla names) correctly
                w.write(join(header));
                w.write("\r\n");
                for (String[] r : rows) {
                    w.write(join(r));
                    w.write("\r\n");
                }
            }
            share(a, f, "text/csv");
        } catch (IOException e) {
            Toast.makeText(a, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String join(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(csvCell(cells[i]));
        }
        return sb.toString();
    }

    private static String csvCell(String v) {
        if (v == null) v = "";
        if (!v.isEmpty() && "=+@".indexOf(v.charAt(0)) >= 0) v = "'" + v; // formula-injection guard
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    // ---------------- PDF ----------------
    public static void exportPdf(Activity a, String fileName, String title, String subtitle,
                                 String[] header, List<String[]> rows, float[] weights) {
        PdfDocument doc = new PdfDocument();
        try {
            float totalW = 0;
            for (float w : weights) totalW += w;
            float usable = PAGE_W - 2 * MARGIN;
            float[] xs = new float[weights.length];
            float[] ws = new float[weights.length];
            float x = MARGIN;
            for (int i = 0; i < weights.length; i++) {
                xs[i] = x;
                ws[i] = usable * weights[i] / totalW;
                x += ws[i];
            }

            Paint titleP = new Paint(Paint.ANTI_ALIAS_FLAG);
            titleP.setTextSize(18);
            titleP.setFakeBoldText(true);
            Paint subP = new Paint(Paint.ANTI_ALIAS_FLAG);
            subP.setTextSize(11);
            subP.setColor(Color.DKGRAY);
            Paint headP = new Paint(Paint.ANTI_ALIAS_FLAG);
            headP.setTextSize(12);
            headP.setFakeBoldText(true);
            Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);
            textP.setTextSize(12);
            Paint lineP = new Paint();
            lineP.setColor(Color.LTGRAY);
            lineP.setStrokeWidth(1);

            int pageNo = 0;
            PdfDocument.Page page = null;
            Canvas c = null;
            float y = 0;

            for (String[] row : rows) {
                if (page == null || y + ROW_H > PAGE_H - MARGIN) {
                    if (page != null) doc.finishPage(page);
                    page = doc.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNo).create());
                    c = page.getCanvas();
                    y = MARGIN;
                    if (pageNo == 1) {
                        c.drawText(title, MARGIN, y + 16, titleP);
                        y += 28;
                        c.drawText(subtitle, MARGIN, y + 10, subP);
                        y += 24;
                    }
                    drawRow(c, headP, header, xs, ws, y + 14);
                    y += ROW_H;
                    c.drawLine(MARGIN, y - 6, PAGE_W - MARGIN, y - 6, lineP);
                }
                drawRow(c, textP, row, xs, ws, y + 14);
                y += ROW_H;
            }
            if (page != null) doc.finishPage(page);

            File f = new File(exportsDir(a), safe(fileName));
            try (FileOutputStream out = new FileOutputStream(f)) {
                doc.writeTo(out);
            }
            share(a, f, "application/pdf");
        } catch (IOException e) {
            Toast.makeText(a, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            doc.close();
        }
    }

    private static void drawRow(Canvas c, Paint p, String[] cells, float[] xs, float[] ws, float baseline) {
        for (int i = 0; i < cells.length && i < xs.length; i++) {
            String t = cells[i] == null ? "" : cells[i];
            int n = p.breakText(t, true, ws[i] - 6, null);
            c.drawText(t, 0, n, xs[i], baseline, p);
        }
    }

    // ---------------- Share ----------------
    private static void share(Activity a, File file, String mime) {
        Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", file);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.setClipData(ClipData.newRawUri("", uri));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(Intent.createChooser(i, "Save / Share"));
    }
}