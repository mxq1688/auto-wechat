package com.wechatassistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.wechatassistant.R
import com.wechatassistant.manager.SettingsManager
import java.io.File
import java.io.FileOutputStream

class VoiceSettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    
    // 数据
    private val contactsMap = mutableMapOf<String, MutableList<String>>()
    private val contactPhotos = mutableMapOf<String, String>()
    private val wakeWordList = mutableListOf<String>()
    private val videoKeywordList = mutableListOf<String>()
    private val voiceKeywordList = mutableListOf<String>()
    private val generalKeywordList = mutableListOf<String>()
    
    // 当前选择照片的联系人
    private var currentPhotoContact: String? = null
    private var tempPhotoFile: File? = null
    
    // 拍照结果处理
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoFile != null) {
            currentPhotoContact?.let { contact ->
                val savedPath = savePhoto(tempPhotoFile!!, contact)
                if (savedPath != null) {
                    contactPhotos[contact] = savedPath
                    settings.setContactPhoto(contact, savedPath)
                    refreshContactList()
                    Toast.makeText(this, "照片已保存", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 相册选择结果处理
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentPhotoContact?.let { contact ->
                val savedPath = savePhotoFromUri(uri, contact)
                if (savedPath != null) {
                    contactPhotos[contact] = savedPath
                    settings.setContactPhoto(contact, savedPath)
                    refreshContactList()
                    Toast.makeText(this, "照片已保存", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            currentPhotoContact?.let { takePhoto() }
        } else {
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 容器
    private lateinit var contactListContainer: LinearLayout
    private lateinit var wakeWordListContainer: LinearLayout
    private lateinit var videoKeywordListContainer: LinearLayout
    private lateinit var voiceKeywordListContainer: LinearLayout
    private lateinit var generalKeywordListContainer: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_settings)
        
        settings = SettingsManager.getInstance(this)
        
        initViews()
        loadData()
        setupListeners()
    }
    
    // LLM 相关 View
    private lateinit var switchLLM: Switch
    private lateinit var llmConfigContainer: LinearLayout
    private lateinit var editLLMApiUrl: EditText
    private lateinit var editLLMApiKey: EditText
    
    private fun initViews() {
        contactListContainer = findViewById(R.id.contactListContainer)
        wakeWordListContainer = findViewById(R.id.wakeWordListContainer)
        videoKeywordListContainer = findViewById(R.id.videoKeywordListContainer)
        voiceKeywordListContainer = findViewById(R.id.voiceKeywordListContainer)
        generalKeywordListContainer = findViewById(R.id.generalKeywordListContainer)
        
        // LLM 配置
        switchLLM = findViewById(R.id.switchLLM)
        llmConfigContainer = findViewById(R.id.llmConfigContainer)
        editLLMApiUrl = findViewById(R.id.editLLMApiUrl)
        editLLMApiKey = findViewById(R.id.editLLMApiKey)
    }
    
    private fun loadData() {
        // 加载联系人
        contactsMap.clear()
        settings.getContacts().forEach { (name, aliases) ->
            contactsMap[name] = aliases.toMutableList()
        }
        
        // 加载联系人照片
        contactPhotos.clear()
        contactPhotos.putAll(settings.getContactPhotos())
        
        // 加载唤醒词
        wakeWordList.clear()
        wakeWordList.addAll(settings.getWakeWords())
        
        // 加载关键词
        videoKeywordList.clear()
        videoKeywordList.addAll(settings.getVideoCallKeywords())
        
        voiceKeywordList.clear()
        voiceKeywordList.addAll(settings.getVoiceCallKeywords())
        
        generalKeywordList.clear()
        generalKeywordList.addAll(settings.getGeneralCallKeywords())
        
        // 设置唤醒词开关
        findViewById<CheckBox>(R.id.checkRequireWakeWord).isChecked = settings.requireWakeWord
        
        // 加载 LLM 配置
        switchLLM.isChecked = settings.llmEnabled
        editLLMApiUrl.setText(settings.llmApiUrl)
        editLLMApiKey.setText(settings.llmApiKey)
        llmConfigContainer.visibility = if (settings.llmEnabled) android.view.View.VISIBLE else android.view.View.GONE
        
        // 刷新界面
        refreshContactList()
        refreshTagList(wakeWordListContainer, wakeWordList, 0xFFFF9800.toInt())
        refreshTagList(videoKeywordListContainer, videoKeywordList, 0xFF2196F3.toInt())
        refreshTagList(voiceKeywordListContainer, voiceKeywordList, 0xFF4CAF50.toInt())
        refreshTagList(generalKeywordListContainer, generalKeywordList, 0xFFFF9800.toInt())
    }
    
    private fun setupListeners() {
        // 返回
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        // 保存
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveData()
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
        
        // 添加联系人
        findViewById<Button>(R.id.btnAddContact).setOnClickListener {
            val editName = findViewById<EditText>(R.id.editNewWechatName)
            val name = editName.text.toString().trim()
            if (name.isNotEmpty() && !contactsMap.containsKey(name)) {
                contactsMap[name] = mutableListOf(name)
                refreshContactList()
                editName.text.clear()
            } else if (contactsMap.containsKey(name)) {
                Toast.makeText(this, "该联系人已存在", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 添加唤醒词
        findViewById<Button>(R.id.btnAddWakeWord).setOnClickListener {
            val editWord = findViewById<EditText>(R.id.editNewWakeWord)
            val word = editWord.text.toString().trim()
            if (word.isNotEmpty() && !wakeWordList.contains(word)) {
                wakeWordList.add(word)
                addTag(wakeWordListContainer, wakeWordList, word, 0xFFFF9800.toInt())
                editWord.text.clear()
            }
        }
        
        // 添加视频关键词
        findViewById<Button>(R.id.btnAddVideoKeyword).setOnClickListener {
            val editWord = findViewById<EditText>(R.id.editNewVideoKeyword)
            val word = editWord.text.toString().trim()
            if (word.isNotEmpty() && !videoKeywordList.contains(word)) {
                videoKeywordList.add(word)
                addTag(videoKeywordListContainer, videoKeywordList, word, 0xFF2196F3.toInt())
                editWord.text.clear()
            }
        }
        
        // 添加语音关键词
        findViewById<Button>(R.id.btnAddVoiceKeyword).setOnClickListener {
            val editWord = findViewById<EditText>(R.id.editNewVoiceKeyword)
            val word = editWord.text.toString().trim()
            if (word.isNotEmpty() && !voiceKeywordList.contains(word)) {
                voiceKeywordList.add(word)
                addTag(voiceKeywordListContainer, voiceKeywordList, word, 0xFF4CAF50.toInt())
                editWord.text.clear()
            }
        }
        
        // 添加通用关键词
        findViewById<Button>(R.id.btnAddGeneralKeyword).setOnClickListener {
            val editWord = findViewById<EditText>(R.id.editNewGeneralKeyword)
            val word = editWord.text.toString().trim()
            if (word.isNotEmpty() && !generalKeywordList.contains(word)) {
                generalKeywordList.add(word)
                addTag(generalKeywordListContainer, generalKeywordList, word, 0xFFFF9800.toInt())
                editWord.text.clear()
            }
        }
        
        // LLM 开关
        switchLLM.setOnCheckedChangeListener { _, isChecked ->
            llmConfigContainer.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }
        
        // 恢复默认
        findViewById<Button>(R.id.btnResetDefault).setOnClickListener {
            settings.setWakeWords(SettingsManager.DEFAULT_WAKE_WORDS)
            settings.requireWakeWord = false
            settings.setVideoCallKeywords(SettingsManager.DEFAULT_VIDEO_KEYWORDS)
            settings.setVoiceCallKeywords(SettingsManager.DEFAULT_VOICE_KEYWORDS)
            settings.setGeneralCallKeywords(SettingsManager.DEFAULT_GENERAL_KEYWORDS)
            settings.setContacts(emptyMap())
            loadData()
            Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveData() {
        settings.setContacts(contactsMap)
        settings.setWakeWords(wakeWordList.toSet())
        settings.requireWakeWord = findViewById<CheckBox>(R.id.checkRequireWakeWord).isChecked
        settings.setVideoCallKeywords(videoKeywordList.toSet())
        settings.setVoiceCallKeywords(voiceKeywordList.toSet())
        settings.setGeneralCallKeywords(generalKeywordList.toSet())
        
        // 保存 LLM 配置
        settings.llmEnabled = switchLLM.isChecked
        settings.llmApiUrl = editLLMApiUrl.text.toString().trim()
        settings.llmApiKey = editLLMApiKey.text.toString().trim()
    }
    
    private fun refreshContactList() {
        contactListContainer.removeAllViews()
        
        if (contactsMap.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "暂无联系人，请在下方添加"
                textSize = 14f
                setTextColor(0xFF999999.toInt())
                setPadding(0, 16, 0, 16)
            }
            contactListContainer.addView(emptyView)
            return
        }
        
        contactsMap.forEach { (wechatName, aliases) ->
            // 联系人卡片
            val cardView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(0xFFF5F5F5.toInt())
                setPadding(16, 12, 16, 12)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            
            // 头部：照片 + 微信名 + 删除按钮
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            
            // 联系人照片
            val photoView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(64, 64).apply { marginEnd = 12 }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFFE0E0E0.toInt())
                
                val photoPath = contactPhotos[wechatName]
                if (photoPath != null && File(photoPath).exists()) {
                    val bitmap = BitmapFactory.decodeFile(photoPath)
                    setImageBitmap(bitmap)
                } else {
                    setImageResource(android.R.drawable.ic_menu_camera)
                }
                
                setOnClickListener {
                    showPhotoOptionsDialog(wechatName)
                }
            }
            
            val nameView = TextView(this).apply {
                text = "📱 $wechatName"
                textSize = 16f
                setTextColor(0xFF1976D2.toInt())
                setTypeface(null, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            
            val deleteContactBtn = TextView(this).apply {
                text = "删除"
                textSize = 13f
                setTextColor(0xFFE53935.toInt())
                setPadding(16, 8, 0, 8)
                setOnClickListener {
                    contactsMap.remove(wechatName)
                    settings.removeContactPhoto(wechatName)
                    contactPhotos.remove(wechatName)
                    refreshContactList()
                }
            }
            
            headerRow.addView(photoView)
            headerRow.addView(nameView)
            headerRow.addView(deleteContactBtn)
            cardView.addView(headerRow)
            
            // 简称标签区域
            val aliasContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            
            aliases.forEach { alias ->
                val tag = TextView(this).apply {
                    text = "  $alias  ×"
                    textSize = 13f
                    setTextColor(0xFFFFFFFF.toInt())
                    setBackgroundColor(0xFF42A5F5.toInt())
                    setPadding(12, 6, 12, 6)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginEnd = 8 }
                    setOnClickListener {
                        aliases.remove(alias)
                        if (aliases.isEmpty()) {
                            contactsMap.remove(wechatName)
                        }
                        refreshContactList()
                    }
                }
                aliasContainer.addView(tag)
            }
            
            cardView.addView(aliasContainer)
            
            // 添加简称输入区
            val addAliasRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            
            val aliasInput = EditText(this).apply {
                hint = "添加简称..."
                textSize = 14f
                setPadding(12, 8, 12, 8)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            
            val addAliasBtn = TextView(this).apply {
                text = " +添加 "
                textSize = 13f
                setTextColor(0xFF4CAF50.toInt())
                setPadding(16, 8, 8, 8)
                setOnClickListener {
                    val newAlias = aliasInput.text.toString().trim()
                    if (newAlias.isNotEmpty() && !aliases.contains(newAlias)) {
                        aliases.add(newAlias)
                        aliasInput.text.clear()
                        refreshContactList()
                    }
                }
            }
            
            addAliasRow.addView(aliasInput)
            addAliasRow.addView(addAliasBtn)
            cardView.addView(addAliasRow)
            
            contactListContainer.addView(cardView)
        }
    }
    
    private fun refreshTagList(container: LinearLayout, list: MutableList<String>, bgColor: Int) {
        container.removeAllViews()
        list.forEach { addTag(container, list, it, bgColor) }
    }
    
    private fun addTag(container: LinearLayout, list: MutableList<String>, text: String, bgColor: Int) {
        val tag = TextView(this).apply {
            this.text = "  $text  ×"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(bgColor)
            setPadding(14, 8, 14, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 8; bottomMargin = 4 }
            setOnClickListener {
                list.remove(text)
                container.removeView(this)
            }
        }
        container.addView(tag)
    }
    
    // ==================== 照片相关方法 ====================
    
    private fun showPhotoOptionsDialog(contactName: String) {
        currentPhotoContact = contactName
        
        val options = arrayOf("📷 拍照", "🖼️ 从相册选择", "❌ 删除照片")
        AlertDialog.Builder(this)
            .setTitle("设置 $contactName 的照片")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> pickImageFromGallery()
                    2 -> {
                        contactPhotos.remove(contactName)
                        settings.removeContactPhoto(contactName)
                        refreshContactList()
                        Toast.makeText(this, "照片已删除", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun takePhoto() {
        try {
            val photoDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "contacts")
            if (!photoDir.exists()) photoDir.mkdirs()
            
            tempPhotoFile = File(photoDir, "temp_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                tempPhotoFile!!
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动相机: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }
    
    private fun savePhoto(sourceFile: File, contactName: String): String? {
        return try {
            val photoDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "contacts")
            if (!photoDir.exists()) photoDir.mkdirs()
            
            val destFile = File(photoDir, "${contactName}_${System.currentTimeMillis()}.jpg")
            
            // 读取原图
            var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            
            // 读取EXIF方向并旋转
            bitmap = rotateImageIfRequired(bitmap, sourceFile.absolutePath)
            
            // 居中裁剪成正方形再缩放（不变形）
            val croppedBitmap = centerCropToSquare(bitmap, 400)
            FileOutputStream(destFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            // 回收原图
            if (bitmap != croppedBitmap) bitmap.recycle()
            
            // 删除临时文件
            sourceFile.delete()
            
            destFile.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "保存照片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    private fun savePhotoFromUri(uri: Uri, contactName: String): String? {
        return try {
            val photoDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "contacts")
            if (!photoDir.exists()) photoDir.mkdirs()
            
            val destFile = File(photoDir, "${contactName}_${System.currentTimeMillis()}.jpg")
            
            contentResolver.openInputStream(uri)?.use { input ->
                var bitmap = BitmapFactory.decodeStream(input)
                
                // 尝试读取EXIF方向
                contentResolver.openInputStream(uri)?.use { exifInput ->
                    val exif = ExifInterface(exifInput)
                    bitmap = rotateImageByExif(bitmap, exif)
                }
                
                // 居中裁剪成正方形再缩放（不变形）
                val croppedBitmap = centerCropToSquare(bitmap, 400)
                FileOutputStream(destFile).use { out ->
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                // 回收原图
                if (bitmap != croppedBitmap) bitmap.recycle()
            }
            
            destFile.absolutePath
        } catch (e: Exception) {
            Toast.makeText(this, "保存照片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
    
    /**
     * 居中裁剪成正方形并缩放
     */
    private fun centerCropToSquare(bitmap: Bitmap, targetSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 先裁剪成正方形
        val squareSize = minOf(width, height)
        val x = (width - squareSize) / 2
        val y = (height - squareSize) / 2
        
        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, squareSize, squareSize)
        
        // 再缩放到目标尺寸
        return if (squareSize != targetSize) {
            val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true)
            if (croppedBitmap != bitmap && croppedBitmap != scaledBitmap) {
                croppedBitmap.recycle()
            }
            scaledBitmap
        } else {
            croppedBitmap
        }
    }
    
    /**
     * 根据文件路径读取EXIF并旋转图片
     */
    private fun rotateImageIfRequired(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            rotateImageByExif(bitmap, exif)
        } catch (e: Exception) {
            bitmap
        }
    }
    
    /**
     * 根据EXIF信息旋转图片
     */
    private fun rotateImageByExif(bitmap: Bitmap, exif: ExifInterface): Bitmap {
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        
        return if (rotationDegrees != 0f) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }
}

