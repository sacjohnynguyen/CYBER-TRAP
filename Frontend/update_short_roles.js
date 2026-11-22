const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

try {
    const userHome = process.env.USERPROFILE || 'C:\\Users\\Windows';
    const downloadsPath = path.join(userHome, 'Downloads');
    const docxPath = path.join(downloadsPath, 'Nhom3.docx');

    const tempZip = path.join(__dirname, 'temp_nhom3_short.zip');
    const tempDir = path.join(__dirname, 'temp_nhom3_short_dir');

    if (fs.existsSync(tempZip)) fs.unlinkSync(tempZip);
    if (fs.existsSync(tempDir)) fs.rmSync(tempDir, { recursive: true, force: true });

    fs.copyFileSync(docxPath, tempZip);
    fs.mkdirSync(tempDir);

    execSync(`powershell -Command "Expand-Archive -Path '${tempZip}' -DestinationPath '${tempDir}' -Force"`);

    const xmlPath = path.join(tempDir, 'word', 'document.xml');
    if (fs.existsSync(xmlPath)) {
        let xmlContent = fs.readFileSync(xmlPath, 'utf8');

        console.log("Co Sang (DB) khong?:", xmlContent.includes('Sang (DB)'));
        console.log("Co Đức (UI) khong?:", xmlContent.includes('Đức (UI)'));
        console.log("Co Sang (Backend) khong?:", xmlContent.includes('Sang (Backend)'));

        // Swap short roles
        // Sang (DB) -> Đức (DB)
        // Đức (UI) -> Sang (UI)
        // Sang (Backend) -> Đức (Backend)

        xmlContent = xmlContent.replace(/Sang \(DB\)/g, 'Đức (DB)');
        xmlContent = xmlContent.replace(/Đức \(UI\)/g, 'Sang (UI)');
        xmlContent = xmlContent.replace(/Sang \(Backend\)/g, 'Đức (Backend)');

        // Also check if there are other occurrences like "Đức (Backend)", "Sang (Frontend)"
        xmlContent = xmlContent.replace(/Đức \(Frontend\)/g, 'Sang (Frontend)');
        xmlContent = xmlContent.replace(/Sang \(Back-End\)/g, 'Đức (Back-End)');

        fs.writeFileSync(xmlPath, xmlContent, 'utf8');
        console.log("✅ Đã cập nhật xong tất cả nhãn vai trò ngắn gọn (DB, UI, Backend, Mobile)!");
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
