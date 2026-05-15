package com.activityplatform.backend.reporting.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SimpleXlsxWorkbookBuilder {
  private static final int EXCEL_MAX_CELL_LENGTH = 32767;
  private static final int EXCEL_MAX_SHEET_NAME_LENGTH = 31;

  public byte[] build(List<Sheet> sheets) {
    if (sheets == null || sheets.isEmpty()) {
      throw new IllegalArgumentException("At least one sheet is required.");
    }

    List<Sheet> normalizedSheets = normalizeSheets(sheets);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
      write(zip, "[Content_Types].xml", contentTypesXml(normalizedSheets.size()));
      write(zip, "_rels/.rels", rootRelationshipsXml());
      write(zip, "xl/workbook.xml", workbookXml(normalizedSheets));
      write(zip, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml(normalizedSheets.size()));

      for (int index = 0; index < normalizedSheets.size(); index += 1) {
        write(
            zip,
            "xl/worksheets/sheet" + (index + 1) + ".xml",
            worksheetXml(normalizedSheets.get(index))
        );
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to build XLSX workbook.", exception);
    }

    return output.toByteArray();
  }

  private List<Sheet> normalizeSheets(List<Sheet> sheets) {
    List<Sheet> normalizedSheets = new ArrayList<>();
    Set<String> usedNames = new HashSet<>();

    for (Sheet sheet : sheets) {
      String name = uniqueSheetName(sheet.name(), usedNames);
      List<List<String>> rows = sheet.rows() == null ? List.of() : sheet.rows();
      normalizedSheets.add(new Sheet(name, rows, sheet.oddHeader(), sheet.oddFooter()));
    }

    return normalizedSheets;
  }

  private String uniqueSheetName(String rawName, Set<String> usedNames) {
    String baseName = sanitizeSheetName(rawName);
    String name = baseName;
    int suffix = 2;

    while (usedNames.contains(name.toLowerCase())) {
      String suffixText = " " + suffix;
      int baseLength = Math.min(
          baseName.length(),
          EXCEL_MAX_SHEET_NAME_LENGTH - suffixText.length()
      );
      name = baseName.substring(0, baseLength) + suffixText;
      suffix += 1;
    }

    usedNames.add(name.toLowerCase());
    return name;
  }

  private String sanitizeSheetName(String rawName) {
    String name = rawName == null || rawName.isBlank() ? "Sheet" : rawName.trim();
    name = name.replaceAll("[:\\\\/?*\\[\\]]", " ");
    name = name.replaceAll("\\s+", " ").trim();

    if (name.isBlank()) {
      name = "Sheet";
    }

    if (name.length() > EXCEL_MAX_SHEET_NAME_LENGTH) {
      name = name.substring(0, EXCEL_MAX_SHEET_NAME_LENGTH);
    }

    return name;
  }

  private String contentTypesXml(int sheetCount) {
    StringBuilder xml = new StringBuilder();
    xml.append("""
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
        <Default Extension="xml" ContentType="application/xml"/>
        <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
        """);

    for (int index = 1; index <= sheetCount; index += 1) {
      xml.append("<Override PartName=\"/xl/worksheets/sheet")
          .append(index)
          .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
    }

    xml.append("</Types>");
    return xml.toString();
  }

  private String rootRelationshipsXml() {
    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
        """;
  }

  private String workbookXml(List<Sheet> sheets) {
    StringBuilder xml = new StringBuilder();
    xml.append("""
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
        <sheets>
        """);

    for (int index = 0; index < sheets.size(); index += 1) {
      xml.append("<sheet name=\"")
          .append(escapeXml(sheets.get(index).name()))
          .append("\" sheetId=\"")
          .append(index + 1)
          .append("\" r:id=\"rId")
          .append(index + 1)
          .append("\"/>");
    }

    xml.append("</sheets></workbook>");
    return xml.toString();
  }

  private String workbookRelationshipsXml(int sheetCount) {
    StringBuilder xml = new StringBuilder();
    xml.append("""
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        """);

    for (int index = 1; index <= sheetCount; index += 1) {
      xml.append("<Relationship Id=\"rId")
          .append(index)
          .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
          .append(index)
          .append(".xml\"/>");
    }

    xml.append("</Relationships>");
    return xml.toString();
  }

  private String worksheetXml(Sheet sheet) {
    StringBuilder xml = new StringBuilder();
    xml.append("""
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <sheetData>
        """);

    for (int rowIndex = 0; rowIndex < sheet.rows().size(); rowIndex += 1) {
      List<String> row = sheet.rows().get(rowIndex);
      int excelRowIndex = rowIndex + 1;
      xml.append("<row r=\"").append(excelRowIndex).append("\">");

      for (int columnIndex = 0; columnIndex < row.size(); columnIndex += 1) {
        xml.append("<c r=\"")
            .append(columnName(columnIndex))
            .append(excelRowIndex)
            .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
            .append(escapeXml(cellValue(row.get(columnIndex))))
            .append("</t></is></c>");
      }

      xml.append("</row>");
    }

    xml.append("</sheetData>");
    if (hasText(sheet.oddHeader()) || hasText(sheet.oddFooter())) {
      xml.append("<headerFooter>");
      if (hasText(sheet.oddHeader())) {
        xml.append("<oddHeader>")
            .append(escapeXml(sheet.oddHeader()))
            .append("</oddHeader>");
      }
      if (hasText(sheet.oddFooter())) {
        xml.append("<oddFooter>")
            .append(escapeXml(sheet.oddFooter()))
            .append("</oddFooter>");
      }
      xml.append("</headerFooter>");
    }
    xml.append("</worksheet>");
    return xml.toString();
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String columnName(int index) {
    StringBuilder name = new StringBuilder();
    int value = index + 1;

    while (value > 0) {
      int remainder = (value - 1) % 26;
      name.insert(0, (char) ('A' + remainder));
      value = (value - 1) / 26;
    }

    return name.toString();
  }

  private String cellValue(String value) {
    if (value == null) {
      return "";
    }

    return value.length() <= EXCEL_MAX_CELL_LENGTH
        ? value
        : value.substring(0, EXCEL_MAX_CELL_LENGTH);
  }

  private String escapeXml(String value) {
    StringBuilder escaped = new StringBuilder();

    for (int index = 0; index < value.length(); index += 1) {
      char character = value.charAt(index);
      switch (character) {
        case '&' -> escaped.append("&amp;");
        case '<' -> escaped.append("&lt;");
        case '>' -> escaped.append("&gt;");
        case '"' -> escaped.append("&quot;");
        case '\'' -> escaped.append("&apos;");
        default -> {
          if (isValidXmlCharacter(character)) {
            escaped.append(character);
          }
        }
      }
    }

    return escaped.toString();
  }

  private boolean isValidXmlCharacter(char character) {
    return character == 0x9
        || character == 0xA
        || character == 0xD
        || character >= 0x20;
  }

  private void write(ZipOutputStream zip, String name, String contents) throws IOException {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(contents.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  public record Sheet(
      String name,
      List<List<String>> rows,
      String oddHeader,
      String oddFooter
  ) {
    public Sheet(String name, List<List<String>> rows) {
      this(name, rows, null, null);
    }
  }
}
