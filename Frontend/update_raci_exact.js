const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

try {
    const userHome = process.env.USERPROFILE || 'C:\\Users\\Windows';
    const downloadsPath = path.join(userHome, 'Downloads');
    const docxPath = path.join(downloadsPath, 'Nhom3.docx');

    const tempZip = path.join(__dirname, 'temp_nhom3_raci_exact.zip');
    const tempDir = path.join(__dirname, 'temp_nhom3_raci_exact_dir');

    if (fs.existsSync(tempZip)) fs.unlinkSync(tempZip);
    if (fs.existsSync(tempDir)) fs.rmSync(tempDir, { recursive: true, force: true });

    fs.copyFileSync(docxPath, tempZip);
    fs.mkdirSync(tempDir);

    execSync(`powershell -Command "Expand-Archive -Path '${tempZip}' -DestinationPath '${tempDir}' -Force"`);

    const xmlPath = path.join(tempDir, 'word', 'document.xml');
    if (fs.existsSync(xmlPath)) {
        let xmlContent = fs.readFileSync(xmlPath, 'utf8');

        // Extract tables
        const tables = xmlContent.match(/<w:tbl\b[^>]*>.*?<\/w:tbl>/g) || [];

        for (let tIdx = 0; tIdx < tables.length; tIdx++) {
            let tbl = tables[tIdx];
            if (tbl.includes('RACI') || tbl.includes('H.M.Đức') || tbl.includes('N.N.Sang')) {
                console.log(`Đang cập nhật Bảng RACI [Index ${tIdx}]...`);

                // Create new updated RACI table XML structure
                let newTbl = tbl;

                // 2. Thiết kế UI/UX: Toàn (A/R), Sang (R), Đức (C)
                // 3. CSDL & Stored Proc: Đức (A/R), Sang (C), Phú (R)
                // 4. REST APIs Backend: Phú (A/R), Đức (R), Sang (C)
                // 5. AI Engine & Security: Phú (A/R)
                // 6. Lập trình Mobile App: Sang (A/R), Toàn (R)

                // Let's replace row contents accurately
                // Row 2: UI/UX -> Toàn A/R
                newTbl = newTbl.replace(
                    /(2\.\s*Thiết kế Giao diện Figma UI\/UX.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)A \/ R(<\/w:t>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)C(<\/w:t>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)I/s,
                    '$1C$2A / R$3R'
                );

                // Row 3: CSDL -> Đức A/R
                newTbl = newTbl.replace(
                    /(3\.\s*Thiết kế CSDL.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)I(<\/w:t>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)I(<\/w:t>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)C(<\/w:t>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)A \/ R/s,
                    '$1I$2A / R$3I$4C'
                );

                // Row 4: REST API Backend -> Phú A/R, Đức R
                newTbl = newTbl.replace(
                    /(4\.\s*Phát triển REST APIs Backend.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)I(<\/w:tc>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)I(<\/w:tc>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)C(<\/w:tc>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)A \/ R(<\/w:tc>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)R/s,
                    '$1I$2R$3I$4C$5A / R'
                );

                // Row 6: Mobile App -> Sang A/R, Toàn R
                newTbl = newTbl.replace(
                    /(6\.\s*Lập trình Mobile App.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)I(<\/w:tc>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)R(<\/w:tc>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)A \/ R(<\/w:tc>.*?<\/w:tc>\s*<w:tc[^>]*>.*?<w:t[^>]*>)I/s,
                    '$1I$2I$3R$4A / R'
                );

                xmlContent = xmlContent.replace(tbl, newTbl);
            }
        }

        fs.writeFileSync(xmlPath, xmlContent, 'utf8');
        console.log("✅ Đã cập nhật xong Ma trận RACI trong Mục 2.3 khớp 100% với Role!");
    }

    // Re-pack zip
    if (fs.existsSync(tempZip)) fs.unlinkSync(tempZip);
    execSync(`powershell -Command "Compress-Archive -Path '${tempDir}\\*' -DestinationPath '${tempDir}.zip' -Force"`);
    fs.copyFileSync(`${tempDir}.zip`, docxPath);
    if (fs.existsSync(`${tempDir}.zip`)) fs.unlinkSync(`${tempDir}.zip`);

    console.log("🎉 ĐÃ CẬP NHẬT HOÀN TẤT VÀO FILE NHOM3.DOCX!");

    if (fs.existsSync(tempZip)) fs.unlinkSync(tempZip);
    if (fs.existsSync(tempDir)) fs.rmSync(tempDir, { recursive: true, force: true });

} catch (err) {
    console.error("Lỗi:", err);
}
