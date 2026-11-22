const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

try {
    const docxPath = 'D:\\Thay Hoang\\Bao_Cao_Du_An_CYBER_TRAP.docx';
    const tempZip = path.join(__dirname, 'temp_doc.zip');
    const tempDir = path.join(__dirname, 'temp_docx');

    if (fs.existsSync(tempZip)) fs.unlinkSync(tempZip);
    if (fs.existsSync(tempDir)) fs.rmSync(tempDir, { recursive: true, force: true });

    fs.copyFileSync(docxPath, tempZip);
    fs.mkdirSync(tempDir);
    execSync(`powershell -Command "Expand-Archive -Path '${tempZip}' -DestinationPath '${tempDir}' -Force"`);

    const xmlPath = path.join(tempDir, 'word', 'document.xml');
    if (fs.existsSync(xmlPath)) {
        const xml = fs.readFileSync(xmlPath, 'utf8');

        console.log("=== BÀI HỌC CÁ NHÂN (PARAGRAPHS 330 - 340) ===");
        const paragraphs = xml.match(/<w:p\b[^>]*>.*?<\/w:p>/g) || [];
        for (let i = 330; i < Math.min(342, paragraphs.length); i++) {
            const p = paragraphs[i];
            const texts = p.match(/<w:t\b[^>]*>(.*?)<\/w:t>/g) || [];
            const textStr = texts.map(t => t.replace(/<[^>]+>/g, '')).join('');
            console.log(`[${i}] ${textStr}`);
        }
    }

    if (fs.existsSync(tempZip)) fs.unlinkSync(tempZip);
    if (fs.existsSync(tempDir)) fs.rmSync(tempDir, { recursive: true, force: true });

} catch (err) {
    console.error("Loi:", err);
}
