package com.bingo.backend.pdf;

import com.bingo.backend.card.BingoCard;
import com.bingo.backend.common.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PdfService {
    private static final Color GREEN = new Color(0x1E, 0x6F, 0x43);
    private static final Color GREEN_LIGHT = new Color(0x3F, 0x8F, 0x56);
    private static final Color BLUE = new Color(0x1F, 0x4E, 0x79);
    private static final Color BLUE_LIGHT = new Color(0x2D, 0x6E, 0xA3);
    private static final Color ORANGE = new Color(0xE6, 0x7E, 0x22);
    private static final Color TERRACOTTA = new Color(0x8B, 0x2F, 0x1C);
    private static final Color GOLD = new Color(0xD6, 0xA8, 0x32);
    private static final Color INK = new Color(0x0B, 0x3A, 0x53);
    private static final Color PAPER = new Color(0xFF, 0xFD, 0xF8);
    private static final Color DETAIL = new Color(0xF4, 0xF6, 0xF7);
    private static final Color LINE = new Color(0xC7, 0xD5, 0xDA);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public byte[] generate(BingoCard card) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // increase top margin so header doesn't feel cramped
            Document document = new Document(PageSize.A4, 19, 19, 30, 13);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new TicketPageEvent());
            document.open();

            document.add(header(card));
            document.add(dataBand(card));
            document.add(boardFrame(card));
            document.add(eventBand());
            document.add(footerBand());

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el PDF");
        }
    }

    private PdfPTable header(BingoCard card) throws Exception {
        PdfPTable wrapper = new PdfPTable(2);
        wrapper.setWidthPercentage(94);
        wrapper.setHorizontalAlignment(Element.ALIGN_CENTER);
        wrapper.setWidths(new float[]{72, 28});
        wrapper.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        wrapper.setSpacingAfter(1);

        PdfPCell titleCell = noBorderCell();
        // add some padding so title moves down from top
        titleCell.setPaddingTop(8);
        Paragraph titleMain = new Paragraph();
        titleMain.add(new Chunk("GRAN ", font(52, Font.BOLDITALIC, TERRACOTTA)));
        titleMain.add(new Chunk("BINGO", font(58, Font.BOLD, INK)));
        titleMain.setAlignment(Element.ALIGN_CENTER);
        titleMain.setSpacingBefore(8);
        titleMain.setLeading(56);
        titleCell.addElement(titleMain);
        Paragraph titleBottom = new Paragraph("URP", font(48, Font.BOLD, GREEN));
        titleBottom.setAlignment(Element.ALIGN_CENTER);
        titleBottom.setLeading(42);
        titleBottom.setSpacingBefore(4);
        titleCell.addElement(titleBottom);

        // logos under the title, constrained to title column width
        PdfPTable logos = new PdfPTable(3);
        logos.setWidthPercentage(92);
        logos.setWidths(new float[]{1,1,1});
        logos.addCell(logoCell("static/logos/geourp.png", 160, 64, 0));
        logos.addCell(logoCell("static/logos/civial.png", 96, 44, 0));
        logos.addCell(logoCell("static/logos/aci.png", 112, 56, 0));
        titleCell.addElement(logos);

        wrapper.addCell(titleCell);

        PdfPCell ticketCell = noBorderCell();
        ticketCell.setBackgroundColor(DETAIL);
        ticketCell.setMinimumHeight(142);
        ticketCell.setPadding(3);
        ticketCell.addElement(centerText("CARTÓN N°", font(10, Font.BOLD, PAPER), BLUE, 7));
        PdfPCell serialPanel = new PdfPCell(new Phrase(card.getSerial(), font(22, Font.BOLD, PAPER)));
        serialPanel.setBorder(Rectangle.NO_BORDER);
        serialPanel.setBackgroundColor(ORANGE);
        serialPanel.setCellEvent(new SoftShadowEvent(ORANGE, 4, 1.4f));
        serialPanel.setHorizontalAlignment(Element.ALIGN_CENTER);
        serialPanel.setVerticalAlignment(Element.ALIGN_MIDDLE);
        serialPanel.setFixedHeight(28);
        serialPanel.setPadding(0);
        PdfPTable serialTable = new PdfPTable(1);
        serialTable.setWidthPercentage(100);
        serialTable.addCell(serialPanel);
        ticketCell.addElement(serialTable);

        Image qr = qrImage(frontendUrl + "/verificar/" + card.getSerial());
        qr.scaleToFit(76, 76);
        qr.setAlignment(Element.ALIGN_CENTER);
        PdfPCell qrPanel = noBorderCell();
        qrPanel.setPaddingTop(0);
        qrPanel.setPaddingBottom(0);
        qrPanel.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrPanel.setCellEvent(new SoftShadowEvent(PAPER, 2, 1.0f));
        qrPanel.addElement(qr);
        PdfPTable qrTable = new PdfPTable(1);
        qrTable.setWidthPercentage(74);
        qrTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrTable.addCell(qrPanel);
        ticketCell.addElement(qrTable);

        Paragraph qrText = new Paragraph("Escanea para validar", font(7, Font.BOLD, INK));
        qrText.setAlignment(Element.ALIGN_CENTER);
        qrText.setSpacingBefore(0);
        qrText.setLeading(8);
        ticketCell.addElement(qrText);
        wrapper.addCell(ticketCell);

        return wrapper;
    }

    private PdfPTable dataBand(BingoCard card) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{30, 30, 18, 22});
        table.setSpacingAfter(3);
        table.addCell(infoCell("Comprador", card.getBuyerName(), "\u25CB", true));
        table.addCell(infoCell("Representante", card.getAssignedTo() == null ? "-" : card.getAssignedTo().getFullName(), "\u25C8", true));
        table.addCell(infoCell("Agrupación", card.getOrganizationCode().name(), "\u25CF", true));
        table.addCell(infoCell("Fecha emisión", card.getAssignedAt() == null ? "-" : DATE_FORMAT.format(card.getAssignedAt()), "\u25A3", false));
        return table;
    }

    private PdfPTable boardFrame(BingoCard card) throws Exception {
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(100);
        wrapper.setSpacingBefore(0);
        wrapper.setSpacingAfter(6);

        PdfPCell cell = noBorderCell();
        cell.setPadding(4);
        cell.setCellEvent(new BoardFrameEvent());
        cell.addElement(boardTable(card));
        wrapper.addCell(cell);
        return wrapper;
    }

    private PdfPTable boardTable(BingoCard card) throws Exception {
        List<List<String>> rows = objectMapper.readValue(card.getNumbersJson(), new TypeReference<>() {
        });

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1, 1, 1});
        table.setSpacingBefore(0);
        table.setSpacingAfter(0);
        table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        List<Color> headerColors = List.of(BLUE, GREEN, new Color(0x00, 0x6B, 0x4A), ORANGE, TERRACOTTA);
        List<String> letters = List.of("B", "I", "N", "G", "O");
        for (int i = 0; i < letters.size(); i++) {
            PdfPCell cell = new PdfPCell(new Phrase(letters.get(i), font(34, Font.BOLD, PAPER)));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setBackgroundColor(headerColors.get(i));
            cell.setFixedHeight(50);
            cell.setBorderColor(GREEN);
            cell.setBorderWidth(1.2f);
            cell.setPadding(0);
            table.addCell(cell);
        }

        for (List<String> row : rows) {
            for (String value : row) {
                PdfPCell cell;
                if ("LIBRE".equals(value)) {
                    cell = freeCell();
                } else {
                    cell = new PdfPCell(new Phrase(value, font(34, Font.BOLD, INK)));
                    cell.setBackgroundColor(PAPER);
                }
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setFixedHeight(80);
                cell.setBorderColor(new Color(0x68, 0x9B, 0x70));
                cell.setBorderWidth(0.65f);
                cell.setPadding(0);
                table.addCell(cell);
            }
        }
        return table;
    }

    private PdfPTable eventBand() throws Exception {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(92);
        table.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.setWidths(new float[]{1, 1, 1});
        table.setSpacingAfter(1);
        table.addCell(eventCell("\u25A3", "SÁBADO 20\nDE JUNIO", BLUE, true));
        table.addCell(eventCell("\u25F7", "01:00 PM", GREEN, true));
        table.addCell(eventCell("\u25CE", "TRANSMISIÓN\nVIRTUAL", ORANGE, false));
        return table;
    }

    private PdfPTable logoBand() throws Exception {
        // Wrap logos into a 2-column table so they occupy only the title column width (72/28)
        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(94);
        outer.setHorizontalAlignment(Element.ALIGN_CENTER);
        outer.setWidths(new float[]{72, 28});

        PdfPCell left = noBorderCell();
        left.setPaddingTop(0);
        // top separator line inside the left column only
        PdfPCell line = transparentCell();
        line.setBorder(Rectangle.TOP);
        line.setBorderColor(LINE);
        line.setPaddingTop(4);
        line.setPaddingBottom(2);
        left.addElement(line);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1});
        table.addCell(logoCell("static/logos/geourp.png", 220, 88, 0));
        table.addCell(logoCell("static/logos/civial.png", 88, 38, 0));
        table.addCell(logoCell("static/logos/aci.png", 112, 56, 5));
        left.addElement(table);

        PdfPCell right = transparentCell();
        // keep right column empty so logos align under the title column
        outer.addCell(left);
        outer.addCell(right);

        return outer;
    }

    private PdfPTable footerBand() throws Exception {
        PdfPTable wrapper = new PdfPTable(1);
        wrapper.setWidthPercentage(92);
        wrapper.setHorizontalAlignment(Element.ALIGN_CENTER);
        wrapper.setSpacingBefore(6);

        PdfPCell line = transparentCell();
        line.setBorder(Rectangle.TOP);
        line.setBorderColor(LINE);
        line.setPaddingTop(6);
        line.setPaddingBottom(4);
        wrapper.addCell(line);

        // footer phrase in uniform cursive-like font
        Font cursive = new Font(Font.TIMES_ROMAN, 36, Font.ITALIC, BLUE);
        Paragraph phrase = new Paragraph();
        phrase.add(new Chunk("Feliz día, papá.", cursive));
        phrase.setAlignment(Element.ALIGN_CENTER);
        PdfPCell phraseCell = transparentCell();
        phraseCell.setPaddingTop(8);
        phraseCell.addElement(phrase);
        wrapper.addCell(phraseCell);

        return wrapper;
    }

    private PdfPCell infoCell(String label, String value, String icon, boolean divider) {
        PdfPCell cell = noBorderCell();
        cell.setBorder(divider ? Rectangle.RIGHT | Rectangle.BOTTOM : Rectangle.BOTTOM);
        cell.setBorderColor(LINE);
        cell.setPadding(3);
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(icon + "  ", font(14, Font.BOLD, BLUE)));
        paragraph.add(new Chunk(label.toUpperCase() + ":\n", font(8, Font.BOLD, INK)));
        paragraph.add(new Chunk(value == null ? "-" : value, font(10, Font.BOLD, GREEN)));
        paragraph.setLeading(12);
        cell.addElement(paragraph);
        return cell;
    }

    private PdfPCell freeCell() {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk("* * *\n", font(9, Font.BOLD, GOLD)));
        paragraph.add(new Chunk("LIBRE\n", font(15, Font.BOLD, PAPER)));
        paragraph.add(new Chunk("* * *", font(10, Font.BOLD, GOLD)));
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setLeading(16);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PAPER);
        cell.setCellEvent(new FreeBadgeEvent());
        cell.addElement(paragraph);
        return cell;
    }

    private PdfPCell eventCell(String icon, String text, Color color, boolean divider) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(icon + "\n", font(14, Font.BOLD, color)));
        paragraph.add(new Chunk(text, font(12, Font.BOLD, INK)));
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setLeading(13);

        PdfPCell cell = noBorderCell();
        if (divider) {
            cell.setBorder(Rectangle.RIGHT);
            cell.setBorderColor(LINE);
        }
        cell.setPadding(0);
        cell.addElement(paragraph);
        return cell;
    }

    private PdfPCell logoCell(String resourcePath, float maxWidth, float maxHeight, float lift) throws Exception {
        Image logo = trimmedLogoImage(resourcePath);
        logo.scaleToFit(maxWidth, maxHeight);
        logo.setAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = transparentCell();
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(0);
        cell.setPaddingBottom(lift);
        cell.setPaddingLeft(0);
        cell.setPaddingRight(0);
        cell.setFixedHeight(64);
        cell.addElement(logo);
        return cell;
    }

    private Image trimmedLogoImage(String resourcePath) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream input = resource.getInputStream()) {
            BufferedImage source = ImageIO.read(input);
            if (source == null) {
                return Image.getInstance(resource.getContentAsByteArray());
            }

            int minX = source.getWidth();
            int minY = source.getHeight();
            int maxX = -1;
            int maxY = -1;

            for (int y = 0; y < source.getHeight(); y++) {
                for (int x = 0; x < source.getWidth(); x++) {
                    int argb = source.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    int red = (argb >>> 16) & 0xFF;
                    int green = (argb >>> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    boolean visible = alpha > 18 && !(red > 245 && green > 245 && blue > 245);
                    if (visible) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }

            if (maxX < minX || maxY < minY) {
                return Image.getInstance(resource.getContentAsByteArray());
            }

            int padding = 8;
            minX = Math.max(0, minX - padding);
            minY = Math.max(0, minY - padding);
            maxX = Math.min(source.getWidth() - 1, maxX + padding);
            maxY = Math.min(source.getHeight() - 1, maxY + padding);

            BufferedImage cropped = source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
            BufferedImage copy = new BufferedImage(cropped.getWidth(), cropped.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = copy.createGraphics();
            graphics.drawImage(cropped, 0, 0, null);
            graphics.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(copy, "png", out);
            return Image.getInstance(out.toByteArray());
        }
    }

    private Paragraph centerText(String text, Font font, Color background, float padding) {
        Chunk chunk = new Chunk("  " + text + "  ", font);
        chunk.setBackground(background, padding, 2, padding, 3);
        Paragraph paragraph = new Paragraph(chunk);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        return paragraph;
    }

    private PdfPCell noBorderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(PAPER);
        return cell;
    }

    private PdfPCell transparentCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell borderedCell(Color border, float width, Color background) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(border);
        cell.setBorderWidth(width);
        cell.setBackgroundColor(background);
        return cell;
    }

    private PdfPCell coloredCell(Color background) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(background);
        return cell;
    }

    private Font font(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private Image qrImage(String value) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(value, BarcodeFormat.QR_CODE, 190, 190);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Image.getInstance(out.toByteArray());
    }

    private static class TicketPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle page = document.getPageSize();
            PdfContentByte under = writer.getDirectContentUnder();
            under.saveState();
            under.setColorFill(PAPER);
            under.rectangle(page.getLeft(), page.getBottom(), page.getWidth(), page.getHeight());
            under.fill();
            under.setColorStroke(new Color(0xF0, 0xEA, 0xDC));
            under.setLineWidth(0.35f);
            for (int i = 0; i < 18; i++) {
                under.moveTo(page.getLeft() + 22 + (i * 9), page.getTop() - 26);
                under.lineTo(page.getLeft() + 8, page.getTop() - 92 - (i * 3));
                under.moveTo(page.getRight() - 22 - (i * 9), page.getBottom() + 26);
                under.lineTo(page.getRight() - 8, page.getBottom() + 92 + (i * 3));
            }
            under.stroke();
            under.restoreState();

            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();
            cb.setLineWidth(1.1f);
            cb.setColorStroke(INK);
            cb.rectangle(page.getLeft() + 10, page.getBottom() + 10, page.getWidth() - 20, page.getHeight() - 20);
            cb.stroke();
            cb.setLineWidth(0.55f);
            cb.setColorStroke(GREEN);
            cb.rectangle(page.getLeft() + 14, page.getBottom() + 14, page.getWidth() - 28, page.getHeight() - 28);
            cb.stroke();
            drawCorner(cb, page.getLeft() + 18, page.getTop() - 18, 1, -1);
            drawCorner(cb, page.getRight() - 18, page.getTop() - 18, -1, -1);
            drawCorner(cb, page.getLeft() + 18, page.getBottom() + 18, 1, 1);
            drawCorner(cb, page.getRight() - 18, page.getBottom() + 18, -1, 1);
            cb.restoreState();
        }

        private void drawCorner(PdfContentByte cb, float x, float y, int xDir, int yDir) {
            cb.setLineCap(PdfContentByte.LINE_CAP_PROJECTING_SQUARE);
            cb.setLineWidth(4f);
            cb.setColorStroke(INK);
            cb.moveTo(x, y);
            cb.lineTo(x + (xDir * 32), y);
            cb.stroke();
            cb.setColorStroke(GREEN);
            cb.moveTo(x, y);
            cb.lineTo(x, y + (yDir * 32));
            cb.stroke();
            cb.setLineWidth(3f);
            cb.setColorStroke(ORANGE);
            cb.moveTo(x + (xDir * 8), y + (yDir * 8));
            cb.lineTo(x + (xDir * 34), y + (yDir * 34));
            cb.stroke();
        }
    }

    private static class RoundedBorderEvent implements PdfPCellEvent {
        private final Color border;
        private final Color accent;
        private final float radius;
        private final float width;
        private final boolean shadow;

        RoundedBorderEvent(Color border, Color accent, float radius, float width, boolean shadow) {
            this.border = border;
            this.accent = accent;
            this.radius = radius;
            this.width = width;
            this.shadow = shadow;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            float left = position.getLeft() + 5;
            float bottom = position.getBottom() + 5;
            float widthValue = position.getWidth() - 10;
            float heightValue = position.getHeight() - 10;
            PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
            cb.saveState();
            if (shadow) {
                cb.setLineWidth(3.0f);
                cb.setColorStroke(new Color(0xD8, 0xDE, 0xDE));
                cb.roundRectangle(left + 2, bottom - 2, widthValue, heightValue, radius);
                cb.stroke();
            }
            cb.setLineWidth(width);
            cb.setColorStroke(border);
            cb.roundRectangle(left, bottom, widthValue, heightValue, radius);
            cb.stroke();
            cb.setLineWidth(1.2f);
            cb.setColorStroke(accent);
            cb.moveTo(left + 12, bottom + heightValue - 12);
            cb.lineTo(left + widthValue - 12, bottom + heightValue - 12);
            cb.stroke();
            cb.restoreState();
        }
    }

    private static class RoundedFillEvent implements PdfPCellEvent {
        private final Color fill;
        private final float radius;

        RoundedFillEvent(Color fill, float radius) {
            this.fill = fill;
            this.radius = radius;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte bg = canvases[PdfPTable.BACKGROUNDCANVAS];
            bg.saveState();
            bg.setColorFill(fill);
            bg.roundRectangle(position.getLeft(), position.getBottom(), position.getWidth(), position.getHeight(), radius);
            bg.fill();
            bg.restoreState();
        }
    }

    private static class TicketShadowEvent implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte bg = canvases[PdfPTable.BACKGROUNDCANVAS];
            bg.saveState();
            bg.setColorFill(new Color(0xD8, 0xDE, 0xDE));
            bg.roundRectangle(position.getLeft() + 4, position.getBottom() - 2,
                    position.getWidth() - 8, position.getHeight() - 10, 12);
            bg.fill();
            bg.setColorFill(DETAIL);
            bg.roundRectangle(position.getLeft() + 5, position.getBottom() + 5,
                    position.getWidth() - 10, position.getHeight() - 10, 12);
            bg.fill();
            bg.restoreState();
        }
    }

    private static class SoftShadowEvent implements PdfPCellEvent {
        private final Color fill;
        private final float radius;
        private final float offset;

        SoftShadowEvent(Color fill, float radius, float offset) {
            this.fill = fill;
            this.radius = radius;
            this.offset = offset;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte bg = canvases[PdfPTable.BACKGROUNDCANVAS];
            bg.saveState();
            bg.setColorFill(new Color(0xD9, 0xDE, 0xDD));
            bg.roundRectangle(position.getLeft() + offset, position.getBottom() - offset,
                    position.getWidth() - 1, position.getHeight(), radius);
            bg.fill();
            bg.setColorFill(fill);
            bg.roundRectangle(position.getLeft(), position.getBottom(),
                    position.getWidth(), position.getHeight(), radius);
            bg.fill();
            bg.restoreState();
        }
    }

    private static class BoardFrameEvent implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.LINECANVAS];
            cb.saveState();
            cb.setLineWidth(2.3f);
            cb.setColorStroke(GREEN);
            cb.roundRectangle(position.getLeft() + 1.5f, position.getBottom() + 1.5f,
                    position.getWidth() - 3, position.getHeight() - 3, 13);
            cb.stroke();
            cb.setLineWidth(1.0f);
            cb.setColorStroke(INK);
            cb.roundRectangle(position.getLeft() + 4, position.getBottom() + 4,
                    position.getWidth() - 8, position.getHeight() - 8, 10);
            cb.stroke();
            cb.setColorFill(ORANGE);
            float[][] points = {
                    {position.getLeft() + 5, position.getTop() - 5},
                    {position.getRight() - 5, position.getTop() - 5},
                    {position.getLeft() + 5, position.getBottom() + 5},
                    {position.getRight() - 5, position.getBottom() + 5}
            };
            for (float[] point : points) {
                cb.rectangle(point[0] - 2.5f, point[1] - 2.5f, 5, 5);
                cb.fill();
            }
            cb.restoreState();
        }
    }

    private static class FreeBadgeEvent implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            float centerX = (position.getLeft() + position.getRight()) / 2;
            float centerY = (position.getBottom() + position.getTop()) / 2;
            float radius = Math.min(position.getWidth(), position.getHeight()) * 0.42f;
            PdfContentByte bg = canvases[PdfPTable.BACKGROUNDCANVAS];
            bg.saveState();
            bg.setColorFill(new Color(0xD6, 0xDC, 0xD8));
            bg.circle(centerX + 1.7f, centerY - 1.7f, radius);
            bg.fill();
            bg.setColorFill(GOLD);
            bg.circle(centerX, centerY, radius);
            bg.fill();
            bg.setColorFill(new Color(0x0E, 0x62, 0x4B));
            bg.circle(centerX, centerY, radius - 4);
            bg.fill();
            bg.restoreState();
        }
    }
}
