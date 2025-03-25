-- Пользователи
CREATE TABLE users (
                       user_id SERIAL PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       password VARCHAR(100) NOT NULL,
                       role VARCHAR(20) NOT NULL CHECK (role IN ('admin', 'employee', 'guest'))
);

-- Регионы
CREATE TABLE regions (
                         region_id SERIAL PRIMARY KEY,
                         region_name VARCHAR(100) NOT NULL
);

-- Заболеваемость (ежедневные данные)
CREATE TABLE cases (
                       case_id SERIAL PRIMARY KEY,
                       region_id INT REFERENCES regions(region_id),
                       date DATE NOT NULL,
                       confirmed INT NOT NULL,
                       deaths INT NOT NULL,
                       recovered INT NOT NULL
);

-- Прогнозы
CREATE TABLE forecasts (
                           forecast_id SERIAL PRIMARY KEY,
                           region_id INT REFERENCES regions(region_id),
                           forecast_date DATE NOT NULL,
                           predicted_cases INT NOT NULL,
                           created_by INT REFERENCES users(user_id),
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Логи действий
CREATE TABLE logs (
                      log_id SERIAL PRIMARY KEY,
                      user_id INT REFERENCES users(user_id),
                      action VARCHAR(100) NOT NULL,
                      timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Настройки системы (для администратора)
CREATE TABLE settings (
                          setting_id SERIAL PRIMARY KEY,
                          setting_name VARCHAR(50) UNIQUE NOT NULL,
                          setting_value VARCHAR(100) NOT NULL
);