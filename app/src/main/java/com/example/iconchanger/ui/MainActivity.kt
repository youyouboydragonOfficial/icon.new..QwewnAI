package com.example.iconchanger.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.iconchanger.R
import com.example.iconchanger.adapter.AppListAdapter
import com.example.iconchanger.adapter.ShortcutListAdapter
import com.example.iconchanger.databinding.ActivityMainBinding
import com.example.iconchanger.model.AppInfo
import com.example.iconchanger.model.IconConfig
import com.example.iconchanger.model.ShortcutInfo
import com.example.iconchanger.util.AppUtils
import com.example.iconchanger.util.GitHubUtils
import com.example.iconchanger.util.ImageUtils
import com.example.iconchanger.util.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * メインアクティビティ
 * アイコン変更、ショートカット作成、アプリ検索機能を提供
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appAdapter: AppListAdapter
    private lateinit var shortcutAdapter: ShortcutListAdapter
    
    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var selectedApp: AppInfo? = null
    private var iconConfig = IconConfig()
    
    private val allApps = mutableListOf<AppInfo>()
    private val shortcuts = mutableListOf<ShortcutInfo>()
    
    private var searchJob: Job? = null

    // 画像選択用ランチャー
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            loadAndProcessImage(it)
        }
    }

    // 権限リクエスト用ランチャー
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(this, R.string.storage_permission_needed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupListeners()
        loadInstalledApps()
        loadShortcuts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupRecyclerViews() {
        // アプリ一覧（検索結果表示用）
        // ショートカット一覧
        shortcutAdapter = ShortcutListAdapter(
            onItemClick = { shortcut ->
                // ショートカットタップ時の処理
            },
            onDeleteClick = { shortcut ->
                deleteShortcut(shortcut)
            }
        )
        
        binding.shortcutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = shortcutAdapter
        }
    }

    private fun setupListeners() {
        // 画像選択ボタン
        binding.selectImageButton.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // スケールスライダー
        binding.scaleSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                iconConfig = iconConfig.copy(scale = value)
                binding.scaleValueText.text = "${String.format("%.1f", value)}x"
                updatePreview()
            }
        }

        // アプリ選択
        binding.appSelectTextView.setOnClickListener {
            showAppSelectionDialog()
        }

        // アイコン付きショートカット作成
        binding.createWithIconButton.setOnClickListener {
            createShortcut(withIcon = true)
        }

        // 名前のみショートカット作成
        binding.createWithNameOnlyButton.setOnClickListener {
            createShortcut(withIcon = false)
        }

        // 更新チェック
        binding.updateButton.setOnClickListener {
            checkForUpdates()
        }

        // 検索入力リスナー
        binding.searchEditText.addTextChangedListener { text ->
            searchJob?.cancel()
            searchJob = lifecycleScope.launch {
                delay(300) // ディバウンス
                val query = text.toString()
                filterApps(query)
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13 以降は READ_MEDIA_IMAGES 権限
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        imagePickerLauncher.launch("image/*")
                    }
                    else -> {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            else -> {
                // Android 12 以前は READ_EXTERNAL_STORAGE 権限
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        imagePickerLauncher.launch("image/*")
                    }
                    else -> {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    private fun loadAndProcessImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                
                selectedBitmap = bitmap
                updatePreview()
                
                // プレビューが更新されたら自動でスケール調整可能に
                binding.scaleSlider.isEnabled = true
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.failed_to_load_image, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePreview() {
        val bitmap = selectedBitmap ?: return
        
        lifecycleScope.launch {
            val processedBitmap = withContext(Dispatchers.Default) {
                ImageUtils.processIcon(
                    context = this@MainActivity,
                    bitmap = bitmap,
                    scale = iconConfig.scale,
                    rotation = iconConfig.rotation,
                    brightness = iconConfig.brightness,
                    contrast = iconConfig.contrast,
                    saturation = iconConfig.saturation
                )
            }
            
            // 円形プレビュー表示
            val circularBitmap = ImageUtils.createCircularBitmap(processedBitmap)
            binding.previewImage.setImageBitmap(circularBitmap)
        }
    }

    private fun loadInstalledApps() {
        lifecycleScope.launch {
            try {
                val apps = AppUtils.getAllInstalledApps(this@MainActivity)
                allApps.clear()
                allApps.addAll(apps)
                
                // アプリ選択用のヒントを設定
                binding.appSelectTextView.hint = "${apps.size} apps available"
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.failed_to_load_apps, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterApps(query: String) {
        lifecycleScope.launch {
            val filteredApps = if (query.isBlank()) {
                allApps
            } else {
                val lowerQuery = query.lowercase()
                allApps.filter { 
                    it.appName.lowercase().contains(lowerQuery) || 
                    it.packageName.lowercase().contains(lowerQuery)
                }
            }
            
            // 最初の 20 件だけ表示（パフォーマンス最適化）
            val displayApps = filteredApps.take(20)
            
            if (displayApps.isEmpty()) {
                binding.appSelectTextView.text = getString(R.string.no_apps_found)
                selectedApp = null
            } else {
                binding.appSelectTextView.text = "${displayApps.first().appName} (+${displayApps.size - 1} more)"
                selectedApp = displayApps.first()
            }
        }
    }

    private fun showAppSelectionDialog() {
        val appNames = allApps.map { "${it.appName} (${it.packageName})" }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.select_app)
            .setItems(appNames.toTypedArray()) { dialog, which ->
                selectedApp = allApps[which]
                binding.appSelectTextView.text = allApps[which].appName
                
                // アプリのアイコンをプレビューに表示（オプション）
                allApps[which].icon?.let { icon ->
                    binding.previewImage.setImageBitmap(ImageUtils.createCircularBitmap(icon))
                }
                
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createShortcut(withIcon: Boolean) {
        val shortcutName = binding.shortcutNameEditText.text.toString().trim()
        
        if (shortcutName.isEmpty()) {
            Toast.makeText(this, R.string.please_enter_shortcut_name, Toast.LENGTH_SHORT).show()
            return
        }

        // アイコン未選択で「アイコン付き」を選択した場合の警告
        if (withIcon && (selectedBitmap == null && selectedApp?.icon == null)) {
            showIconNotSelectedWarning(shortcutName)
            return
        }

        executeCreateShortcut(shortcutName, withIcon)
    }

    private fun showIconNotSelectedWarning(shortcutName: String) {
        MaterialAlertDialogBuilder(this, R.style.Theme_IconChanger_Dialog)
            .setTitle(R.string.icon_not_selected_title)
            .setMessage(R.string.icon_not_selected_message)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                executeCreateShortcut(shortcutName, withIcon = false)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun executeCreateShortcut(shortcutName: String, withIcon: Boolean) {
        lifecycleScope.launch {
            try {
                val appToUse = selectedApp
                val targetPackage = appToUse?.packageName ?: packageName
                val targetClass = getLauncherActivity(targetPackage) ?: "MainActivity"
                
                val shortcutId = UUID.randomUUID().toString()
                var iconPath: String? = null

                // カスタムアイコンがあれば保存
                if (withIcon && selectedBitmap != null) {
                    val iconFile = File(filesDir, "icons/$shortcutId.png")
                    iconFile.parentFile?.mkdirs()
                    
                    val success = ImageUtils.saveBitmapToFile(selectedBitmap!!, iconFile)
                    if (success) {
                        iconPath = iconFile.absolutePath
                    }
                }

                val shortcut = ShortcutInfo(
                    id = shortcutId,
                    label = appToUse?.appName ?: shortcutName,
                    packageName = targetPackage,
                    activityName = targetClass,
                    iconPath = iconPath,
                    customName = shortcutName
                )

                shortcuts.add(shortcut)
                saveShortcuts()
                shortcutAdapter.submitList(shortcuts.toList())

                // ホーム画面にショートカットを追加（Android 8.0 以上）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    addShortcutToHomeScreen(shortcut)
                }

                Toast.makeText(this@MainActivity, "Shortcut created: $shortcutName", Toast.LENGTH_SHORT).show()
                
                // 入力フィールドをクリア
                binding.shortcutNameEditText.text?.clear()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.failed_to_create_shortcut, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLauncherActivity(packageName: String): String? {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.component?.className
        } catch (e: Exception) {
            null
        }
    }

    private fun addShortcutToHomeScreen(shortcut: ShortcutInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            
            if (shortcutManager.isRequestPinShortcutSupported) {
                val info = android.content.pm.ShortcutInfo.Builder(this, shortcut.id)
                    .setShortLabel(shortcut.customName ?: shortcut.label)
                    .setIntent(
                        Intent(Intent.ACTION_MAIN).apply {
                            setClassName(shortcut.packageName, shortcut.activityName)
                        }
                    )
                    .build()
                
                shortcutManager.requestPinShortcut(info, null)
            }
        }
    }

    private fun deleteShortcut(shortcut: ShortcutInfo) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Shortcut")
            .setMessage("Are you sure you want to delete \"${shortcut.customName ?: shortcut.label}\"?")
            .setPositiveButton(R.string.yes) { _, _ ->
                shortcuts.remove(shortcut)
                saveShortcuts()
                shortcutAdapter.submitList(shortcuts.toList())
                
                // アイコンファイルを削除
                if (!shortcut.iconPath.isNullOrBlank()) {
                    File(shortcut.iconPath).delete()
                }
                
                Toast.makeText(this, "Shortcut deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun saveShortcuts() {
        // SharedPreferences に保存（簡易実装）
        val prefs = getSharedPreferences("shortcuts", MODE_PRIVATE)
        val editor = prefs.edit()
        // 実際には Gson などでシリアライズして保存
        editor.putInt("count", shortcuts.size)
        editor.apply()
    }

    private fun loadShortcuts() {
        // SharedPreferences から読み込み（簡易実装）
        val prefs = getSharedPreferences("shortcuts", MODE_PRIVATE)
        val count = prefs.getInt("count", 0)
        // 実際には Gson などでデシリアライズして読み込み
        shortcutAdapter.submitList(shortcuts.toList())
    }

    private fun checkForUpdates() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        binding.updateButton.isEnabled = false
        binding.updateButton.text = "Checking..."

        lifecycleScope.launch {
            try {
                val release = GitHubUtils.getLatestRelease()
                
                if (release != null) {
                    val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName
                    
                    if (release.tagName != "v$currentVersion") {
                        showUpdateDialog(release)
                    } else {
                        Toast.makeText(this@MainActivity, R.string.you_have_latest_version, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
            } finally {
                binding.updateButton.isEnabled = true
                binding.updateButton.text = getString(R.string.check_update)
            }
        }
    }

    private fun showUpdateDialog(release: com.example.iconchanger.model.GitHubRelease) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_available)
            .setMessage(getString(R.string.new_version_found, release.tagName))
            .setPositiveButton(R.string.download_update) { _, _ ->
                // APK をバックグラウンドでダウンロードしてインストール
                downloadAndInstallApk(release)
            }
            .setNegativeButton(R.string.later, null)
            .show()
    }

    /**
     * GitHub から APK をダウンロードしてインストール
     */
    private fun downloadAndInstallApk(release: com.example.iconchanger.model.GitHubRelease) {
        lifecycleScope.launch {
            try {
                binding.updateButton.isEnabled = false
                binding.updateButton.text = "Downloading..."
                
                // リリースから APK アセットを探す
                val apkAsset = release.assets.find { it.contentType == "application/vnd.android.package-archive" }
                    ?: release.assets.find { it.name.endsWith(".apk") }
                
                if (apkAsset == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "No APK found in this release. Opening release page...",
                        Toast.LENGTH_LONG
                    ).show()
                    // フォールバック：ブラウザでリリースページを開く
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                    startActivity(intent)
                    return@launch
                }
                
                // ダウンロード開始
                val downloadId = startDownload(apkAsset.downloadUrl, apkAsset.name)
                
                if (downloadId != -1L) {
                    // ダウンロードマネージャーでダウンロード
                    Toast.makeText(
                        this@MainActivity,
                        "Download started. Check notification for progress.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // ダウンロードマネージャーが使えない場合、OkHttp で直接ダウンロード
                    downloadWithOkHttp(apkAsset.downloadUrl, apkAsset.name)
                }
                
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Download failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                
                // フォールバック：ブラウザでリリースページを開く
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl))
                startActivity(intent)
            } finally {
                binding.updateButton.isEnabled = true
                binding.updateButton.text = getString(R.string.check_update)
            }
        }
    }

    /**
     * DownloadManager を使用して APK をダウンロード
     */
    private fun startDownload(url: String, fileName: String): Long {
        return try {
            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager
            
            val request = android.app.DownloadManager.Request(Uri.parse(url))
                .setTitle("Icon Changer Pro Update")
                .setDescription("Downloading new version...")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    /**
     * OkHttp を使用して APK を直接ダウンロード（DownloadManager が使えない場合）
     */
    private suspend fun downloadWithOkHttp(url: String, fileName: String) {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.android.package-archive")
                .build()
            
            try {
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Download failed: HTTP ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@withContext
                }
                
                val body = response.body ?: return@withContext
                
                // ファイル保存先
                val downloadDir = File(getExternalFilesDir(null), "downloads")
                downloadDir.mkdirs()
                val outputFile = File(downloadDir, fileName)
                
                // ダウンロード実行
                outputFile.outputStream().use { output ->
                    body.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                
                // インストールを促すインテント
                withContext(Dispatchers.Main) {
                    installApk(outputFile)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Download error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * APK ファイルのインストールを促す
     */
    private fun installApk(file: File) {
        if (!file.exists()) {
            Toast.makeText(this, "APK file not found", Toast.LENGTH_SHORT).show()
            return
        }
        
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )
        } else {
            android.net.Uri.fromFile(file)
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(
                    android.provider.Settings.EXTRA_INSTALLER_PACKAGE_NAME,
                    packageName
                )
            }
        }
        
        // インストール権限の確認（Android 8.0 以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Install Permission Required")
                    .setMessage("This app needs permission to install updates. Would you like to grant this permission?")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        settingsIntent.data = android.net.Uri.parse("package:$packageName")
                        startActivity(settingsIntent)
                    }
                    .setNegativeButton("Later", null)
                    .show()
                return
            }
        }
        
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start installation: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
