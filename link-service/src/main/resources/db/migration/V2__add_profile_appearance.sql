ALTER TABLE profiles
  ADD COLUMN background_theme VARCHAR(24) NOT NULL DEFAULT 'aurora' AFTER avatar_url,
  ADD COLUMN button_style VARCHAR(24) NOT NULL DEFAULT 'soft' AFTER background_theme,
  ADD COLUMN font_family VARCHAR(24) NOT NULL DEFAULT 'system' AFTER button_style;
