create database btl_thuchanh;
USE btl_thuchanh;

--  tạo bảng dịch vụ đi kèm
CREATE TABLE DICH_VU_DI_KEM (
  MaDv VARCHAR(255),
  TenDv VARCHAR(255),
  DonViTinh VARCHAR(255),
  DonGia INT,
  PRIMARY KEY (MaDv)
);

-- tạo bảng phòng
CREATE table  PHONG (
  MaPhong VARCHAR(255) PRIMARY KEY,
  LoaiPhong VARCHAR(255),
  SoKhachToiDa INT,
  GiaPhong1Gio INT,
  MoTa VARCHAR(255) NULL
);

-- tạo bảng khách hàng
CREATE TABLE KHACH_HANG (
  MaKH VARCHAR(255) PRIMARy KEY,
  TenKH VARCHAR(255),
  DiaChi VARCHAR(255),
  SoDT VARCHAR(255)
);

-- tạo bảng DAT_PHONG
CREATE TABLE DAT_PHONG (
  MaDatPhong VARCHAR(255) PRIMARY KEY,
  MaPhong VARCHAR(255),
  MaKH VARCHAR(255),
  NgayDat date,
  GioBatDau TIME,
  GioKetThuc TIME,
  TienDatCoc int,
  GhiChu VARCHAR(255) NULL,
  TrangThaiDat ENUM("Đã đặt", "Đã hủy"),
  FOREIGN KEY (MaPhong) REFERENCES PHONG(MaPhong) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (MaKH) REFERENCES KHACH_HANG(MaKH) ON DELETE CASCADE ON UPDATE CASCADE
);

-- tạo bảng chi tiết sử dụng dv
CREATE TABLE CHI_TIET_SU_DUNG_DV (
  MaDatPhong VARCHAR(255),
  MaDV VARCHAR(255),
  SoLuong INT,
  PRIMARY KEY (MaDatPhong, MaDV),
  FOREIGN KEY (MaDatPhong) REFERENCES DAT_PHONG(MaDatPhong) ON DELETE CASCADE ON UPDATE CASCADE,
  FOREIGN KEY (MaDV) REFERENCES DICH_VU_DI_KEM(MaDv) ON DELETE CASCADE ON UPDATE CASCADE
);

--  Insert dữ liệu bảng Phong
INSERT INTO PHONG (MaPhong, LoaiPhong, SoKhachToiDa, GiaPhong1Gio, MoTa) 
VALUES
  ('P0001', 'Loai 1', 20, 60000, ''),
  ('P0002', 'Loai 2', 25, 80000, ''),
  ('P0003', 'Loai 3', 15, 50000, ''),
  ('P0004', 'Loai 4', 20, 50000, '');
 
 --  Insert dữ liệu bảng KHACH_HANG 
INSERT INTO KHACH_HANG (MaKH, TenKH, DiaChi, SoDT)
VALUES
  ('KH0001', 'Nguyen Van A', 'Hoa Xuan', '1111111111'),
  ('KH0002', 'Nguyen Van B', 'Hoa Hai', '1111111112'),
  ('KH0003', 'Phan Van A', 'Cam Le', '1111111113'),
  ('KH0004', 'Phan Van B', 'Hoa Xuan', '1111111114');


-- Insert du lieu bang DICH_VU_DI_KEM

INSERT INTO DICH_VU_DI_KEM (MaDv, TenDv, DonViTinh, DonGia)
VALUES
  ('DV001', 'Bear', 'lon', 10000),
  ('DV002', 'Nuoc Ngot', 'lon', 8000),
  ('DV003', 'Trai Cay', 'dia', 35000),
  ('DV004', 'Khan uot', 'cai', 2000);
  
-- Insert du lieu bang DAT_PHONG
INSERT INTO DAT_PHONG (MaDatPhong, MaPhong, MaKH, NgayDat, GioBatDau, GioKetThuc, TienDatCoc, GhiChu, TrangThaiDat)
VALUES
  ('DP0001', 'P0001', 'KH0002', '2018-03-26', '11:00:00', '13:30:00', 100000, NULL, 'Đã đặt'),
  ('DP0002', 'P0002', 'KH0003', '2018-03-27', '17:15:00', '19:15:00', 50000, NULL, 'Đã hủy'),
  ('DP0003', 'P0003', 'KH0002', '2018-03-26', '20:30:00', '22:15:00', 100000, NULL, 'Đã đặt'),
  ('DP0004', 'P0004', 'KH0001', '2018-04-01', '19:30:00', '21:15:00', 200000, NULL, 'Đã đặt');


-- Insert du lieu bang CHI_TIET_SU_DUNG_DV
INSERT INTO CHI_TIET_SU_DUNG_DV (MaDatPhong, MaDV, SoLuong)
VALUES
  ('DP0001', 'DV001', 20),  
  ('DP0001', 'DV003', 3),  
  ('DP0001', 'DV002', 10), 
  ('DP0002', 'DV002', 10),
  ('DP0002', 'DV003', 1), 
  ('DP0003', 'DV003', 2), 
  ('DP0003', 'DV004', 10);
  
-- Cau 1:
SELECT MaDatPhong, MaDV, SoLuong 
FROM CHI_TIET_SU_DUNG_DV 
WHERE SoLuong > 10 AND SoLuong < 20;

-- Cau 2:
UPDATE PHONG 
SET GiaPhong1Gio = GiaPhong1Gio + 15000 
WHERE SoKhachToiDa > 20;

-- Cau 3:
DELETE FROM DAT_PHONG 
WHERE TrangThaiDat = 'Đã hủy';

-- Cau 4
SELECT TenKH 
FROM KHACH_HANG 
WHERE (TenKH LIKE 'H%' OR TenKH LIKE 'V%' OR TenKH LIKE 'M%') 
AND LENGTH(TenKH) <= 20;

-- Cau 5
SELECT DISTINCT TenKH 
FROM KHACH_HANG;

-- Cau 6
SELECT MaDV, TenDv, DonViTinh, DonGia 
FROM DICH_VU_DI_KEM 
WHERE (DonViTinh = 'dia' AND DonGia > 35000) 
   OR (DonViTinh = 'cai' AND DonGia < 5000);

-- Cau 7
SELECT 
    DP.MaDatPhong, 
    DP.MaPhong, 
    P.LoaiPhong, 
    P.SoKhachToiDa, 
    P.GiaPhong1Gio AS GiaPhong, 
    DP.MaKH, 
    KH.TenKH, 
    KH.SoDT, 
    DP.NgayDat, 
    DP.GioBatDau, 
    DP.GioKetThuc, 
    CTTD.MaDV AS MaDichVu, 
    CTTD.SoLuong, 
    DV.DonGia 
FROM 
    DAT_PHONG DP
JOIN 
    PHONG P ON DP.MaPhong = P.MaPhong
JOIN 
    KHACH_HANG KH ON DP.MaKH = KH.MaKH
LEFT JOIN 
    CHI_TIET_SU_DUNG_DV CTTD ON DP.MaDatPhong = CTTD.MaDatPhong
LEFT JOIN 
    DICH_VU_DI_KEM DV ON CTTD.MaDV = DV.MaDv
WHERE 
    YEAR(DP.NgayDat) = 2018 
    AND P.GiaPhong1Gio < 80000;

-- Cau 8
SELECT 
    DP.MaDatPhong, 
    DP.MaPhong, 
    P.LoaiPhong, 
    P.GiaPhong1Gio AS GiaPhong, 
    KH.TenKH, 
    DP.NgayDat,
    (P.GiaPhong1Gio * TIMESTAMPDIFF(HOUR, DP.GioBatDau, DP.GioKetThuc)) AS TongTienHat,
    COALESCE(SUM(CTTD.SoLuong * DV.DonGia), 0) AS TongTienSuDungDichVu,
    (P.GiaPhong1Gio * TIMESTAMPDIFF(HOUR, DP.GioBatDau, DP.GioKetThuc) + COALESCE(SUM(CTTD.SoLuong * DV.DonGia), 0)) AS TongTienThanhToan
FROM 
    DAT_PHONG DP
JOIN 
    PHONG P ON DP.MaPhong = P.MaPhong
JOIN 
    KHACH_HANG KH ON DP.MaKH = KH.MaKH
LEFT JOIN 
    CHI_TIET_SU_DUNG_DV CTTD ON DP.MaDatPhong = CTTD.MaDatPhong
LEFT JOIN 
    DICH_VU_DI_KEM DV ON CTTD.MaDV = DV.MaDv
GROUP BY 
    DP.MaDatPhong;

-- Cau 9
SELECT 
    KH.MaKH, 
    KH.TenKH, 
    KH.DiaChi, 
    KH.SoDT 
FROM 
    KHACH_HANG KH
JOIN 
    DAT_PHONG DP ON KH.MaKH = DP.MaKH
JOIN 
    PHONG P ON DP.MaPhong = P.MaPhong
WHERE 
    KH.DiaChi = 'Hoa Xuan';

-- Cau 10
SELECT 
    P.MaPhong, 
    P.LoaiPhong, 
    P.SoKhachToiDa, 
    P.GiaPhong1Gio AS GiaPhong, 
    COUNT(DP.MaDatPhong) AS SoLanDat 
FROM 
    PHONG P
JOIN 
    DAT_PHONG DP ON P.MaPhong = DP.MaPhong
WHERE 
    DP.TrangThaiDat = 'Đã đặt'
GROUP BY 
    P.MaPhong
HAVING 
    COUNT(DP.MaDatPhong) > 2;