package com.obraia.backend.report;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * The building blocks of the PDF reports, drawn with OpenPDF in the colors
 * of the website: a header, tables of facts, sections, tables, bullets and
 * a footer with the page number. The writers only decide what goes in.
 */
public final class ReportDocument {

    static final Color INK = new Color(0x2b, 0x26, 0x22);
    static final Color BRICK = new Color(0xb4, 0x53, 0x2a);
    static final Color MUTED = new Color(0x6b, 0x62, 0x5a);
    static final Color LINE = new Color(0xd9, 0xd2, 0xc7);
    static final Color PAPER = new Color(0xf4, 0xef, 0xe7);
    static final Color GOOD = new Color(0x3f, 0x7d, 0x4e);
    static final Color WARNING = new Color(0xb7, 0x7b, 0x12);
    static final Color DANGER = new Color(0xa8, 0x32, 0x2d);

    private static final float MARGIN = 48;

    /** A section that starts lower than this goes to the next page, so its title is not left alone. */
    private static final float MIN_SECTION_SPACE = 150;

    private final Document document;
    private final PdfWriter writer;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final BaseFont regular;
    private final BaseFont bold;

    private ReportDocument(String footerText, String pageLabel) {
        try {
            regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception ex) {
            throw new IllegalStateException("The fonts of the report could not be loaded", ex);
        }
        document = new Document(PageSize.LETTER, MARGIN, MARGIN, MARGIN, MARGIN + 18);
        writer = PdfWriter.getInstance(document, output);
        writer.setPageEvent(new Footer(footerText, pageLabel));
        document.open();
    }

    /**
     * A new report.
     *
     * @param footerText text on the left of every footer
     * @param pageLabel  "Page {page}" in the language of the report
     */
    public static ReportDocument open(String footerText, String pageLabel, String title, String author) {
        ReportDocument report = new ReportDocument(footerText, pageLabel);
        report.document.addTitle(title);
        report.document.addAuthor(author);
        report.document.addCreator("ObraIA");
        return report;
    }

    /** Closes the document and returns its bytes. */
    public byte[] finish() {
        document.close();
        return output.toByteArray();
    }

    // ---- Fonts ----

    Font font(float size, Color color) {
        return new Font(regular, size, Font.NORMAL, color);
    }

    Font boldFont(float size, Color color) {
        return new Font(bold, size, Font.NORMAL, color);
    }

    // ---- Blocks ----

    /** Brand and kind of report on top, the name of the project and the date under it. */
    public void header(String brand, String kicker, String title, String subtitle) {
        PdfPTable top = new PdfPTable(2);
        top.setWidthPercentage(100);
        top.addCell(plain(new Phrase(brand, boldFont(12, BRICK)), Element.ALIGN_LEFT));
        top.addCell(plain(new Phrase(kicker.toUpperCase(), boldFont(8, MUTED)), Element.ALIGN_RIGHT));
        add(top);

        Paragraph name = new Paragraph(title, boldFont(22, INK));
        name.setSpacingBefore(14);
        add(name);
        Paragraph date = new Paragraph(subtitle, font(9.5f, MUTED));
        date.setSpacingBefore(2);
        add(date);
        rule(BRICK, 1.5f, 10);
    }

    /** Pairs of label and value, three per row, on a soft background. */
    public void facts(List<String[]> pairs) {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);
        for (String[] pair : pairs) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setBackgroundColor(PAPER);
            cell.setPadding(7);
            cell.setPaddingBottom(9);
            cell.addElement(new Paragraph(pair[0].toUpperCase(), boldFont(7, MUTED)));
            Paragraph value = new Paragraph(pair[1], boldFont(10, INK));
            value.setSpacingBefore(2);
            cell.addElement(value);
            table.addCell(cell);
        }
        table.completeRow();
        add(table);
    }

    /** A line of small, muted text. */
    public void note(String text) {
        Paragraph note = new Paragraph(text, font(8.5f, MUTED));
        note.setSpacingBefore(8);
        add(note);
    }

    /** Title of a section, with a thin line under it. */
    public void section(String title) {
        if (writer.getVerticalPosition(false) - document.bottom() < MIN_SECTION_SPACE) {
            document.newPage();
        }
        Paragraph heading = new Paragraph(title, boldFont(13, INK));
        heading.setSpacingBefore(18);
        heading.setKeepTogether(true);
        add(heading);
        rule(LINE, 0.75f, 4);
    }

    /** A paragraph of normal text. */
    public void paragraph(String text) {
        Paragraph paragraph = new Paragraph(text, font(10, INK));
        paragraph.setLeading(14);
        paragraph.setSpacingBefore(6);
        add(paragraph);
    }

    /** A small bold heading inside a section. */
    public void subheading(String text) {
        Paragraph heading = new Paragraph(text, boldFont(10, INK));
        heading.setSpacingBefore(10);
        add(heading);
    }

    /** A list with a brick dot in front of each line. */
    public void bullets(List<String> lines) {
        PdfPTable table = new PdfPTable(new float[] { 3, 97 });
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        for (String line : lines) {
            PdfPCell dot = plain(new Phrase("•", boldFont(12, BRICK)), Element.ALIGN_LEFT);
            dot.setPaddingTop(1.5f);
            table.addCell(dot);
            PdfPCell text = plain(new Phrase(line, font(10, INK)), Element.ALIGN_LEFT);
            text.setLeading(0, 1.3f);
            text.setPaddingTop(3);
            table.addCell(text);
        }
        add(table);
    }

    /** Big numbers in a row, each with its label; a value can have its own color. */
    public void figures(List<Figure> figures) {
        PdfPTable table = new PdfPTable(figures.size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        for (Figure figure : figures) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.LEFT);
            cell.setBorderColor(figure.color() == null ? LINE : figure.color());
            cell.setBorderWidth(2.5f);
            cell.setPaddingLeft(9);
            cell.setPaddingBottom(6);
            cell.addElement(new Paragraph(figure.label().toUpperCase(), boldFont(7, MUTED)));
            Paragraph value = new Paragraph(figure.value(),
                    boldFont(figure.value().length() > 16 ? 11 : 15, figure.color() == null ? INK : figure.color()));
            value.setSpacingBefore(2);
            cell.addElement(value);
            table.addCell(cell);
        }
        add(table);
    }

    /** A table with a header row; the columns are sized by the widths. */
    public Table table(float[] widths, String... headers) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setHeaderRows(1);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header.toUpperCase(), boldFont(7, MUTED)));
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(INK);
            cell.setBorderWidth(0.75f);
            cell.setPadding(5);
            table.addCell(cell);
        }
        return new Table(table);
    }

    /** A thin horizontal line. */
    private void rule(Color color, float width, float spacingBefore) {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        line.setSpacingBefore(spacingBefore);
        PdfPCell cell = new PdfPCell(new Phrase(" ", font(1, INK)));
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(color);
        cell.setBorderWidth(width);
        cell.setFixedHeight(1);
        line.addCell(cell);
        add(line);
    }

    private PdfPCell plain(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(0);
        return cell;
    }

    private void add(Element element) {
        document.add(element);
    }

    /** One big number of a row of figures. */
    public record Figure(String label, String value, Color color) {

        public Figure(String label, String value) {
            this(label, value, null);
        }
    }

    /** A table being filled row by row. */
    public final class Table {

        private final PdfPTable table;
        private boolean shaded;

        private Table(PdfPTable table) {
            this.table = table;
        }

        /** A row of text cells; null shows a dash. */
        public Table row(String... values) {
            for (String value : values) {
                cell(new Phrase(value == null ? "—" : value, font(9, INK)), Element.ALIGN_LEFT);
            }
            shaded = !shaded;
            return this;
        }

        /** Starts a row: then call text, colored and bar, once per column. */
        public Table text(String value) {
            cell(new Phrase(value == null ? "—" : value, font(9, INK)), Element.ALIGN_LEFT);
            return this;
        }

        public Table colored(String value, Color color) {
            cell(new Phrase(value, boldFont(9, color == null ? INK : color)), Element.ALIGN_LEFT);
            return this;
        }

        /** A progress bar with its percent, for a value from 0 to 100. */
        public Table bar(BigDecimal percent, String label) {
            cell(new Phrase(label, font(9, INK)), Element.ALIGN_RIGHT,
                    new ProgressBar(percent == null ? 0 : percent.floatValue()));
            return this;
        }

        /** Ends the row started with text, colored or bar. */
        public Table end() {
            shaded = !shaded;
            return this;
        }

        public void close() {
            add(table);
        }

        private void cell(Phrase phrase, int alignment) {
            cell(phrase, alignment, null);
        }

        /** The table keeps a copy of the cell, so the bar is set before adding it. */
        private void cell(Phrase phrase, int alignment, PdfPCellEvent event) {
            PdfPCell cell = new PdfPCell(phrase);
            cell.setCellEvent(event);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setBorderColor(LINE);
            cell.setBorderWidth(0.5f);
            cell.setHorizontalAlignment(alignment);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            cell.setLeading(0, 1.25f);
            if (shaded) {
                cell.setBackgroundColor(PAPER);
            }
            table.addCell(cell);
        }
    }

    /** Draws a bar on the left part of a cell; the percent is written on the right. */
    private static final class ProgressBar implements PdfPCellEvent {

        private final float percent;

        ProgressBar(float percent) {
            this.percent = Math.max(0, Math.min(100, percent));
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            float left = position.getLeft() + 5;
            float width = position.getWidth() * 0.55f;
            float middle = (position.getTop() + position.getBottom()) / 2 - 2.5f;
            canvas.saveState();
            canvas.setColorFill(LINE);
            canvas.roundRectangle(left, middle, width, 5, 2.5f);
            canvas.fill();
            if (percent > 0) {
                canvas.setColorFill(percent >= 100 ? GOOD : BRICK);
                canvas.roundRectangle(left, middle, Math.max(5, width * percent / 100), 5, 2.5f);
                canvas.fill();
            }
            canvas.restoreState();
        }
    }

    /** Name of the project on the left and page number on the right of every page. */
    private final class Footer extends PdfPageEventHelper {

        private final String text;
        private final String pageLabel;

        Footer(String text, String pageLabel) {
            this.text = text;
            this.pageLabel = pageLabel;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document page) {
            PdfContentByte canvas = writer.getDirectContent();
            float y = page.bottomMargin() - 26;
            canvas.saveState();
            canvas.setColorStroke(LINE);
            canvas.setLineWidth(0.5f);
            canvas.moveTo(page.left(), y + 12);
            canvas.lineTo(page.right(), y + 12);
            canvas.stroke();
            canvas.restoreState();
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(text, font(7.5f, MUTED)),
                    page.left(), y, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase(pageLabel.replace("{page}", String.valueOf(writer.getPageNumber())),
                            font(7.5f, MUTED)),
                    page.right(), y, 0);
        }
    }
}
