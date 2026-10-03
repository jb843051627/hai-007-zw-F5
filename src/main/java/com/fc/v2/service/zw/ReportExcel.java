package com.fc.v2.service.zw;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 复测/盯办倒查报表 Excel 写出（通用表头版）。
 *
 * <p>与 {@link ImpExcel#write} 同一套"固定桩"做法：文档属性时刻与 zip 条目时间钉常量，
 * 同一份数据连导两次落盘字节一致，与导出时刻无关。
 *
 * @author fuce
 * @date 2026-10-03
 */
public final class ReportExcel {

    private ReportExcel() {
    }

    /** 按给定表头与已排定顺序的行写出 xlsx；行内不足的格补空，null 写空串 */
    public static byte[] write(String sheetName, List<String> headers, List<List<String>> rows)
            throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        XSSFWorkbook wb = new XSSFWorkbook();
        try {
            java.util.Date peg = java.sql.Timestamp.valueOf("2026-01-01 00:00:00");
            wb.getProperties().getCoreProperties().setCreated(java.util.Optional.of(peg));
            wb.getProperties().getCoreProperties().setModified(java.util.Optional.of(peg));
            Sheet sheet = wb.createSheet(sheetName == null ? "报表" : sheetName);
            Row head = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                head.createCell(c).setCellValue(headers.get(c));
            }
            int r = 1;
            for (List<String> line : rows) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < headers.size(); c++) {
                    String v = c < line.size() && line.get(c) != null ? line.get(c) : "";
                    row.createCell(c).setCellValue(v);
                }
            }
            wb.write(raw);
        } finally {
            wb.close();
        }
        return normalizeZip(raw.toByteArray());
    }

    private static byte[] normalizeZip(byte[] xlsxBytes) throws IOException {
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(xlsxBytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(out);
        ZipEntry e;
        byte[] buf = new byte[4096];
        while ((e = zis.getNextEntry()) != null) {
            ZipEntry ne = new ZipEntry(e.getName());
            ne.setTime(java.sql.Timestamp.valueOf("2026-01-01 00:00:00").getTime());
            zos.putNextEntry(ne);
            int n;
            while ((n = zis.read(buf)) > 0) {
                zos.write(buf, 0, n);
            }
            zos.closeEntry();
            zis.closeEntry();
        }
        zos.close();
        return out.toByteArray();
    }
}
