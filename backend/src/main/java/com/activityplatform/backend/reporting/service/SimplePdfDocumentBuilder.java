package com.activityplatform.backend.reporting.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class SimplePdfDocumentBuilder {
  private static final int LINE_LENGTH = 96;
  private static final int LINES_PER_PAGE = 42;

  byte[] build(String title, List<String> sourceLines) {
    List<String> lines = wrapLines(sourceLines);
    List<List<String>> pages = pages(lines);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    List<Integer> offsets = new ArrayList<>();
    offsets.add(0);

    write(output, "%PDF-1.4\n");
    int pageCount = pages.size();
    int pageTreeObject = 2;
    int fontObject = 3;
    int firstPageObject = 4;
    int objectCount = 3 + pageCount * 2;

    writeObject(output, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>\n");
    writeObject(output, offsets, pageTreeObject, pagesObject(firstPageObject, pageCount));
    writeObject(output, offsets, fontObject, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n");

    for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
      int pageObject = firstPageObject + pageIndex * 2;
      int contentObject = pageObject + 1;
      writeObject(output, offsets, pageObject, pageObject(pageTreeObject, fontObject, contentObject));
      writeStreamObject(
          output,
          offsets,
          contentObject,
          content(title, pages.get(pageIndex), pageIndex + 1, pageCount)
      );
    }

    int xrefOffset = output.size();
    write(output, "xref\n");
    write(output, "0 " + (objectCount + 1) + "\n");
    write(output, "0000000000 65535 f \n");
    for (int objectNumber = 1; objectNumber <= objectCount; objectNumber++) {
      write(output, String.format("%010d 00000 n \n", offsets.get(objectNumber)));
    }
    write(output, "trailer\n");
    write(output, "<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\n");
    write(output, "startxref\n");
    write(output, xrefOffset + "\n");
    write(output, "%%EOF\n");
    return output.toByteArray();
  }

  private List<String> wrapLines(List<String> sourceLines) {
    List<String> lines = new ArrayList<>();
    for (String sourceLine : sourceLines) {
      String line = printable(sourceLine);
      if (line.isBlank()) {
        lines.add("");
        continue;
      }

      while (line.length() > LINE_LENGTH) {
        int breakIndex = line.lastIndexOf(' ', LINE_LENGTH);
        if (breakIndex < 1) {
          breakIndex = LINE_LENGTH;
        }
        lines.add(line.substring(0, breakIndex).trim());
        line = line.substring(breakIndex).trim();
      }
      lines.add(line);
    }
    return lines;
  }

  private List<List<String>> pages(List<String> lines) {
    if (lines.isEmpty()) {
      return List.of(List.of("No report rows available."));
    }

    List<List<String>> pages = new ArrayList<>();
    for (int index = 0; index < lines.size(); index += LINES_PER_PAGE) {
      pages.add(lines.subList(index, Math.min(index + LINES_PER_PAGE, lines.size())));
    }
    return pages;
  }

  private String pagesObject(int firstPageObject, int pageCount) {
    StringBuilder kids = new StringBuilder();
    for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
      kids.append(firstPageObject + pageIndex * 2).append(" 0 R ");
    }
    return "<< /Type /Pages /Kids [ " + kids + "] /Count " + pageCount + " >>\n";
  }

  private String pageObject(int pageTreeObject, int fontObject, int contentObject) {
    return "<< /Type /Page /Parent " + pageTreeObject
        + " 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 "
        + fontObject + " 0 R >> >> /Contents " + contentObject + " 0 R >>\n";
  }

  private byte[] content(String title, List<String> lines, int pageNumber, int pageCount) {
    StringBuilder content = new StringBuilder();
    content.append("BT\n");
    content.append("/F1 16 Tf\n");
    content.append("50 750 Td\n");
    content.append("(").append(escape(title)).append(") Tj\n");
    content.append("/F1 9 Tf\n");
    content.append("0 -18 Td\n");
    content.append("(").append(escape("Page " + pageNumber + " of " + pageCount)).append(") Tj\n");
    content.append("/F1 10 Tf\n");
    content.append("0 -24 Td\n");
    for (String line : lines) {
      content.append("(").append(escape(line)).append(") Tj\n");
      content.append("0 -14 Td\n");
    }
    content.append("ET\n");
    return content.toString().getBytes(StandardCharsets.US_ASCII);
  }

  private void writeObject(
      ByteArrayOutputStream output,
      List<Integer> offsets,
      int objectNumber,
      String body
  ) {
    offsets.add(output.size());
    write(output, objectNumber + " 0 obj\n");
    write(output, body);
    write(output, "endobj\n");
  }

  private void writeStreamObject(
      ByteArrayOutputStream output,
      List<Integer> offsets,
      int objectNumber,
      byte[] stream
  ) {
    offsets.add(output.size());
    write(output, objectNumber + " 0 obj\n");
    write(output, "<< /Length " + stream.length + " >>\n");
    write(output, "stream\n");
    output.writeBytes(stream);
    write(output, "endstream\n");
    write(output, "endobj\n");
  }

  private void write(ByteArrayOutputStream output, String value) {
    output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
  }

  private String escape(String value) {
    return printable(value)
        .replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)");
  }

  private String printable(String value) {
    if (value == null) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    for (int index = 0; index < value.length(); index++) {
      char character = value.charAt(index);
      if (character == '\r' || character == '\n' || character == '\t') {
        builder.append(' ');
      } else if (character >= 32 && character <= 126) {
        builder.append(character);
      } else {
        builder.append('?');
      }
    }
    return builder.toString();
  }
}
