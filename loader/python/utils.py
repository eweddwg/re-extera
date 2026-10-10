class UIRunnable(dynamic_proxy(Runnable)):
    def __init__(self, func):
        super().__init__()
        self.func = func
    def run(self):
        self.func()


def get_local_dex_path():
    pkg = ApplicationLoader.applicationContext.getPackageName()
    return f"/storage/emulated/0/Android/media/{pkg}/classes.dex"



# localization
def _localize(key):
    lang = LocaleController.getInstance().getCurrentLocale().getLanguage()
    strings = {
        "settings":                ("Настройки re:extera", "Налаштування re:extera", "re:extera settings"),
        "channel_dev":             ("Dev-сборки", "Dev-збірки", "Dev builds"),
        "channel_release":         ("Релизные сборки", "Релізні збірки", "Release builds"),
        "check_updates":           ("Проверить обновления", "Перевірити оновлення", "Check for updates"),
        "install_file":            ("Установить из файла", "Встановити з файлу", "Install from file"),
        "update_downloaded":       ("Обновление скачано! Перезапустите приложение, чтобы применить его.",
                                    "Оновлення завантажено! Перезапустіть застосунок, щоб застосувати його.",
                                    "Update downloaded! Restart the app to apply it."),
        "downloading":             ("Скачивание...", "Завантаження...", "Downloading..."),
        "updated_cache":           ("Обновлено из кеша", "Оновлено з кешу", "Updated from cache"),
        "installed":               ("Установка завершена", "Встановлення завершено", "Installation complete"),
        "channel_switch":          ("Канал изменён. Перезапустите приложение.", "Канал змінено. Перезапустіть застосунок.", "Channel changed. Restart the app."),
        "up_to_date":              ("У вас последняя версия", "У вас остання версія", "Already up to date"),
        "file_not_found":          ("Файл не найден", "Файл не знайдено", "File not found"),
        "no_official_update":      ("Для этой версии ещё нет официального обновления re:extera, возможны баги.",
                                    "Для цієї версії ще немає офіційного оновлення re:extera, можливі баги.",
                                    "re:extera doesn't have an official update for this version yet, so expect bugs."),
        "copy_logs":               ("Скопировать логи", "Скопіювати логи", "Copy logs"),
        "logs_copied":             ("Логи скопированы в буфер обмена", "Логи скопійовано в буфер обміну", "Logs copied to clipboard"),
        "update_channel":          ("Канал обновлений", "Канал оновлень", "Update channel"),
        "select_version":          ("Выбрать версию", "Вибрати версію", "Select version"),
        "select_tg_version":       ("Выберите версию Telegram", "Виберіть версію Telegram", "Select Telegram version"),
        "select_release":          ("Выберите релиз", "Виберіть реліз", "Select release"),
        "select_branch":           ("Выберите ветку", "Виберіть гілку", "Select branch"),
        "select_dev_build":        ("Выберите сборку", "Виберіть збірку", "Select build"),
        "no_releases":             ("Релизы не найдены", "Релізи не знайдено", "No releases found"),
        "no_branches":             ("Ветки не найдены", "Гілки не знайдено", "No branches found"),
        "no_runs":                 ("Сборки не найдены", "Збірки не знайдено", "No builds found"),
        "no_dex":                  ("В этом релизе нет DEX", "У цьому релізі немає DEX", "No DEX in this release"),
        "download_failed":         ("Не удалось скачать", "Не вдалося завантажити", "Download failed"),
        "update":                  ("Обновить", "Оновити", "Update"),
        "later":                   ("Позже", "Пізніше", "Later"),
        "update_title":            ("Доступна версия re:extera {version}", "Доступна версія re:extera {version}", "re:extera {version} is available"),
        "no_prev_version":         ("Предыдущая версия для сравнения не найдена.", "Попередня версія для порівняння не знайдена.", "No previous version found to compare."),
        "no_changes":              ("Нет изменений.", "Немає змін.", "No changes."),
        "no_commits":              ("Коммиты не найдены.", "Коміти не знайдено.", "No commits found."),
        "no_changelog":            ("Список изменений недоступен.", "Список змін недоступний.", "Changelog unavailable."),
        "elyx_saved":              ("Обновление re:extera .elyx скачано в папку «Загрузки»! Установите его из файла.",
                                    "Оновлення re:extera .elyx завантажено до папки «Завантаження»! Встановіть його з файлу.",
                                    "re:extera .elyx update downloaded to the Downloads folder! Please install it from the file."),
        "cancel":                  ("Отмена", "Скасувати", "Cancel"),
        "dex_not_loaded":          ("DEX не загружен", "DEX не завантажений", "DEX is not loaded"),
        "no_plugin_logs":          ("Нет логов плагина", "Немає логів плагіна", "No plugin logs"),
        "dex_settings":            ("Настройки DEX", "Налаштування DEX", "DEX settings"),
    }
    idx = 0 if lang == "ru" else (1 if lang == "uk" else 2)
    return strings[key][idx]


# file download handler
class DownloadListener(dynamic_proxy(NotificationCenter.NotificationCenterDelegate)):
    def __init__(self, plugin, account, file_name, download_path, target_path):
        super().__init__()
        self.plugin = plugin
        self.account = account
        self.file_name = file_name
        self.download_path = download_path
        self.target_path = target_path
        self.nc = NotificationCenter.getInstance(account)

    def didReceivedNotification(self, id, account, args):
        try:
            loaded_file_name = str(args[0])
            if loaded_file_name == self.file_name:
                if id == NotificationCenter.fileLoaded:
                    self.plugin.log(f"File {self.file_name} downloaded success")
                    if os.path.exists(self.download_path):
                        shutil.copy2(self.download_path, self.target_path)
                        self.plugin.log(f"Copied to {self.target_path}")
                        BulletinHelper.show_info(_localize("installed"))
                    self.cleanup()
                elif id == NotificationCenter.fileLoadFailed:
                    self.plugin.log(f"File {self.file_name} download failed")
                    self.cleanup()
        except Exception as e:
            self.plugin.log(f"Listener error: {e}")

    def cleanup(self):
        self.nc.removeObserver(self, NotificationCenter.fileLoaded)
        self.nc.removeObserver(self, NotificationCenter.fileLoadFailed)