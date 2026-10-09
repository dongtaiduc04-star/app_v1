# Vận hành v1 trên Azure

v1 dùng **hệ thống GetLink hiện có**, không tạo môi trường, tên miền, database
hay Argo Application thứ hai. Repo cũ giữ nguyên; website dùng chung hiện theo
`helm_v1`.

## Trạng thái đã xác nhận ngày 08-10-2026

- App: `d6a86653ec927e5049de476ee67d0d66e2a9c437` — cả ba job CI/CD thành công.
- Helm: `6f05266eb2e3034f28c8d5492c5c20a1fa1307dc` — Argo dùng `helm_v1`,
  Synced/Healthy; bốn Pod ứng dụng Ready và dùng đúng tag App trên.
- MySQL Ready; chủ dự án xác nhận website hoạt động ổn.

Đây là kết quả lần triển khai đã kiểm tra, không phải trạng thái giám sát liên
tục. Hai secret, `AZURE_SONAR_HOST_URL`, `ENABLE_AZURE_DELIVERY=true` và quyền
Write cho bốn package đã được chủ dự án cấu hình. Bản workflow khôi phục theo
dự án cũ cần thêm **một variable** trước khi push:

**app_v1 → Settings → Secrets and variables → Actions → Variables →
New repository variable**: Name **`HELM_REPO_NAME`**, Value **`helm_v1`**.

Giữ nguyên bốn mục hiện có; không cần secret mới hay commit rỗng kích hoạt lần
nữa. Khi token hết hạn, cập nhật đúng secret trong GitHub, không dán token vào
code, ảnh chụp hay chat.

## Dùng giao diện để chạy và kiểm tra

1. Trong **Azure Portal → Virtual machines**, chọn đúng subscription
   **Azure for Students** (`529b5eb6-35b8-4998-a690-d34b60f28ca7`) và resource
   group `rg-getlink-dtd-portfolio-mw`. Nếu đang Stopped/deallocated, bấm
   **Start** cho đúng hai VM hiện có:
   `vm-getlink-dtd-portfolio` và `vm-getlink-dtd-portfolio-sonarqube`.
   Không tạo VM mới. Khi cần chạy pipeline, mở
   [SonarQube](https://sonar-azure.dongtaiduc.me) và chờ dịch vụ sẵn sàng;
   VM Running chưa tự chứng minh Sonar đã sẵn sàng.
2. Sửa code trong `app_v1`, commit rồi push lên **main**. Mở
   [app_v1 → Actions](https://github.com/dongtaiduc04-star/app_v1/actions),
   chọn commit mới nhất; chờ cả ba job gốc xanh:
   **Build, Test, Checkstyle & SonarQube (PRs)**,
   **Build Docker images and push to GHCR (push to main)**,
   **Update GetLink Helm values**.
   PR chỉ kiểm tra; không dùng nút **Run workflow** vì bản Azure gốc chỉ chạy
   khi push/PR lên `main`, không có trigger thủ công.
3. Trong [helm_v1](https://github.com/dongtaiduc04-star/helm_v1), xem commit
   tự động mới nhất và `helm/getlink-dtd/values-azure.yaml`: cả bốn tag phải là
   SHA đầy đủ của commit App mới. Trong giao diện Argo CD hiện có, nếu bạn đang
   truy cập được, xem Application **getlink-dtd**: nguồn `helm_v1`,
   **Synced / Healthy**. Không tạo Application hoặc mở thêm endpoint Argo.
4. Mở [website GetLink](https://getlink-azure.dongtaiduc.me), đăng nhập, xem
   link/avatar cũ và mở hồ sơ công khai trong cửa sổ ẩn danh. Không cần tạo/xóa
   dữ liệu chỉ để kiểm tra một lần triển khai bình thường.

Nếu job đỏ, mở đúng job và bước đỏ để đọc lỗi; chưa bấm chạy lại hàng loạt,
bỏ Quality Gate, tăng quyền token hay dùng force push. Nếu website không vào
được, kiểm tra trạng thái hai VM trước. Có cơ chế tiết kiệm tự dừng VM khoảng
3 giờ hoặc 01:00 giờ Việt Nam; không mặc định coi hệ thống chạy 24/7. VM chạy
tiêu thụ chi phí/tín dụng Azure; không tự bỏ cơ chế này chỉ để pipeline xanh.

## Những ranh giới cần giữ

- Repo cũ không bị sửa, nhưng hai pipeline vẫn dùng chung bốn GHCR package và
  project Sonar `getlink-dtd`: lần chạy sau có thể thay `latest` và báo cáo
  Sonar. Tránh phát hành từ `app` và `app_v1` đồng thời.
- Website dùng SHA trong **nguồn Helm đang được Argo chọn**, không dùng
  `latest`. Hiện chỉ một Application `getlink-dtd` chọn `helm_v1`; code v1
  thay đổi có thể ảnh hưởng website/database dùng chung.
- Không chạy Terraform apply để phát hành app. Hạ tầng vẫn được vận hành
  thủ công như dự án cũ; không thêm pipeline Terraform trong `infra_v1`.
  Chỉ chuyển quyền vận
  hành Terraform sang v1 sau khi kiểm tra rõ cùng private backend/state,
  chọn **một writer** và không chạy old/v1 đồng thời. Xem
  [quy tắc dùng chung hạ tầng](https://github.com/dongtaiduc04-star/infra_v1/blob/main/azure/SHARED-CONTROL.md).
- Đổi Argo về repo cũ hoặc chọn image cũ không khôi phục dữ liệu/database.
  Với thay đổi schema hoặc dữ liệu, cần backup và phương án rollback riêng.

Chi tiết quyền và đường phát hành:
[Shared Azure delivery](shared-azure-delivery.md).
