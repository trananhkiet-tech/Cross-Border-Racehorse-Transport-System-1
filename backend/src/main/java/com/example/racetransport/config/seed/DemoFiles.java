package com.example.racetransport.config.seed;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;

/** Tạo tệp PDF/PNG minh họa cho dữ liệu demo (không phải giấy tờ thật). */
final class DemoFiles {

    private DemoFiles() { }

    static MultipartFile pdf(String fileName, String... lines) {
        StringBuilder content = new StringBuilder("BT /F1 16 Tf 60 780 Td (IRTMS - DEMO DOCUMENT / TAI LIEU MINH HOA) Tj ET\n");
        int y = 740;
        for (String line : lines) {
            content.append("BT /F1 12 Tf 60 ").append(y).append(" Td (").append(ascii(line)).append(") Tj ET\n");
            y -= 22;
        }
        content.append("BT /F1 10 Tf 60 80 Td (Khong co gia tri phap ly - chi dung cho do an.) Tj ET\n");
        String stream = content.toString();
        String pdf = "%PDF-1.4\n"
                + "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 595 842]/Contents 4 0 R"
                + "/Resources<</Font<</F1 5 0 R>>>>>>endobj\n"
                + "4 0 obj<</Length " + stream.getBytes(StandardCharsets.US_ASCII).length + ">>stream\n" + stream
                + "endstream endobj\n"
                + "5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n"
                + "trailer<</Root 1 0 R>>\n%%EOF\n";
        return new InMemoryFile(fileName, "application/pdf", pdf.getBytes(StandardCharsets.US_ASCII));
    }

    static MultipartFile png(String fileName, String caption) {
        BufferedImage img = new BufferedImage(480, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0x16, 0x32, 0x5C));
        g.fillRect(0, 0, 480, 320);
        g.setColor(new Color(0xE8, 0xEE, 0xF7));
        g.fillRoundRect(24, 24, 432, 272, 16, 16);
        g.setColor(new Color(0x16, 0x32, 0x5C));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString("IRTMS - DEMO PHOTO", 48, 80);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        g.drawString(ascii(caption), 48, 130);
        g.drawString("Anh minh hoa - khong phai anh that", 48, 260);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", out);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return new InMemoryFile(fileName, "image/png", out.toByteArray());
    }

    /** PDF chuẩn chỉ hỗ trợ ASCII với font cơ bản: bỏ dấu tiếng Việt và ký tự đặc biệt. */
    static String ascii(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return n.replaceAll("[^\\x20-\\x7E]", "").replace("(", "[").replace(")", "]").replace("\\", "/");
    }

    /** MultipartFile trong bộ nhớ để gọi lại đúng service upload như người dùng thật. */
    static final class InMemoryFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] bytes;

        InMemoryFile(String name, String contentType, byte[] bytes) {
            this.name = name;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        @Override
        public String getName() { return "file"; }

        @Override
        public String getOriginalFilename() { return name; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return bytes.length == 0; }

        @Override
        public long getSize() { return bytes.length; }

        @Override
        public byte[] getBytes() { return bytes.clone(); }

        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), bytes);
        }
    }
}
