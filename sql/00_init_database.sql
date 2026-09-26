-- ====================================================================
-- 00_init_database.sql
-- 数据库初始化脚本
-- 由 DataGrip 统一执行，创建面试演示数据库 interview_db
-- ====================================================================

CREATE DATABASE IF NOT EXISTS interview_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE interview_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
