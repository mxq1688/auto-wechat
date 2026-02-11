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
import android.content.res.ColorStateList
import android.graphics.Outline
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.wechatassistant.R
import com.wechatassistant.manager.SettingsManager
import java.io.File
import java.io.FileOutputStream

class VoiceSettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    
    // 数据
    private val contactsMap = mutableMapOf<String, MutableList<String>>()
    private val contactPhotos = mutableMapOf<String, String>()
    private val expandedContacts = mutableSetOf<String>()  // 记住展开的联系人
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
        
        // 唤醒词折叠/展开
        val wakeWordContent = findViewById<LinearLayout>(R.id.wakeWordContent)
        val wakeWordArrow = findViewById<TextView>(R.id.wakeWordArrow)
        findViewById<LinearLayout>(R.id.wakeWordHeader).setOnClickListener {
            if (wakeWordContent.visibility == View.VISIBLE) {
                wakeWordContent.visibility = View.GONE
                wakeWordArrow.text = "▼"
            } else {
                wakeWordContent.visibility = View.VISIBLE
                wakeWordArrow.text = "▲"
            }
        }
        
        // 通话关键词折叠/展开
        val callKeywordContent = findViewById<LinearLayout>(R.id.callKeywordContent)
        val callKeywordArrow = findViewById<TextView>(R.id.callKeywordArrow)
        findViewById<LinearLayout>(R.id.callKeywordHeader).setOnClickListener {
            if (callKeywordContent.visibility == View.VISIBLE) {
                callKeywordContent.visibility = View.GONE
                callKeywordArrow.text = "▼"
            } else {
                callKeywordContent.visibility = View.VISIBLE
                callKeywordArrow.text = "▲"
            }
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
                refreshTagList(wakeWordListContainer, wakeWordList, 0xFFFF9800.toInt())
                editWord.text.clear()
            }
        }
        
        // 添加视频关键词
        findViewById<Button>(R.id.btnAddVideoKeyword).setOnClickListener {
            val editWord = findViewById<EditText>(R.id.editNewVideoKeyword)
            val word = editWord.text.toString().trim()
            if (word.isNotEmpty() && !videoKeywordList.contains(word)) {
                videoKeywordList.add(word)
                refreshTagList(videoKeywordListContainer, videoKeywordList, 0xFF2196F3.toInt())
                editWord.text.clear()
            }
        }
        
        // 添加语音关键词
        findViewById<Button>(R.id.btnAddVoiceKeyword).setOnClickListener {
            val editWord = findViewById<EditText>(R.id.editNewVoiceKeyword)
            val word = editWord.text.toString().trim()
            if (word.isNotEmpty() && !voiceKeywordList.contains(word)) {
                voiceKeywordList.add(word)
                refreshTagList(voiceKeywordListContainer, voiceKeywordList, 0xFF4CAF50.toInt())
                editWord.text.clear()
            }
        }
        
        // 添加通用关键词
        findViewById<Button>(R.id.btnAddGeneralKeyword).setOnClickListener {
            val editWord = findViewById<EditText>(R.id.editNewGeneralKeyword)
            val word = editWord.text.toString().trim()
            if (word.isNotEmpty() && !generalKeywordList.contains(word)) {
                generalKeywordList.add(word)
                refreshTagList(generalKeywordListContainer, generalKeywordList, 0xFFFF9800.toInt())
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
    
    /** dp -> px */
    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private fun refreshContactList() {
        contactListContainer.removeAllViews()

        if (contactsMap.isEmpty()) {
            // 空状态
            val emptyLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(24), 0, dp(24))
            }
            val emptyIcon = TextView(this).apply {
                text = "👤"
                textSize = 36f
                gravity = Gravity.CENTER
            }
            val emptyText = TextView(this).apply {
                text = "暂无联系人\n请在下方添加微信好友"
                textSize = 14f
                setTextColor(0xFF9E9E9E.toInt())
                gravity = Gravity.CENTER
                setLineSpacing(dp(4).toFloat(), 1f)
            }
            emptyLayout.addView(emptyIcon)
            emptyLayout.addView(emptyText)
            contactListContainer.addView(emptyLayout)
            return
        }

        contactsMap.forEach { (wechatName, aliases) ->
            // ===== MaterialCardView 卡片 =====
            val card = MaterialCardView(this).apply {
                radius = dp(14).toFloat()
                cardElevation = dp(3).toFloat()
                setCardBackgroundColor(0xFFFFFFFF.toInt())
                strokeWidth = dp(1)
                strokeColor = 0xFFEEEEEE.toInt()
                useCompatPadding = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6) }
            }

            val cardContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            // ===== 头部：圆形照片 + 微信名 + 删除按钮 =====
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            // 联系人照片（圆形）
            val photoSize = dp(52)
            val photoView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(photoSize, photoSize).apply {
                    marginEnd = dp(14)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP

                // 圆形裁剪
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                clipToOutline = true

                val photoPath = contactPhotos[wechatName]
                if (photoPath != null && File(photoPath).exists()) {
                    val bitmap = BitmapFactory.decodeFile(photoPath)
                    setImageBitmap(bitmap)
                    setBackgroundResource(R.drawable.bg_photo_placeholder)
                } else {
                    setBackgroundResource(R.drawable.bg_photo_placeholder)
                    setImageResource(android.R.drawable.ic_menu_camera)
                    imageTintList = ColorStateList.valueOf(0xFF90CAF9.toInt())
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                }

                setOnClickListener { showPhotoOptionsDialog(wechatName) }
            }

            // 名字列
            val nameColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val nameView = TextView(this).apply {
                text = wechatName
                textSize = 17f
                setTextColor(0xFF212121.toInt())
                setTypeface(null, Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            val subtitleView = TextView(this).apply {
                text = "微信名 · ${aliases.size}个简称"
                textSize = 12f
                setTextColor(0xFFBDBDBD.toInt())
                setPadding(0, dp(2), 0, 0)
            }
            nameColumn.addView(nameView)
            nameColumn.addView(subtitleView)

            // 删除按钮
            val deleteBtnSize = dp(34)
            val deleteBtn = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(deleteBtnSize, deleteBtnSize)
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                imageTintList = ColorStateList.valueOf(0xFFBDBDBD.toInt())
                setPadding(dp(7), dp(7), dp(7), dp(7))
                // 获取 ripple 背景
                val outValue = TypedValue()
                context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless, outValue, true
                )
                setBackgroundResource(outValue.resourceId)
                setOnClickListener {
                    AlertDialog.Builder(this@VoiceSettingsActivity)
                        .setTitle("删除联系人")
                        .setMessage("确定要删除「$wechatName」及其所有简称吗？")
                        .setPositiveButton("删除") { _, _ ->
                            contactsMap.remove(wechatName)
                            settings.removeContactPhoto(wechatName)
                            contactPhotos.remove(wechatName)
                            refreshContactList()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
            }

            // 展开/收起箭头
            val isExpanded = expandedContacts.contains(wechatName)
            val arrowView = TextView(this).apply {
                text = if (isExpanded) "▲" else "▼"
                textSize = 12f
                setTextColor(0xFFBDBDBD.toInt())
                setPadding(dp(6), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            headerRow.addView(photoView)
            headerRow.addView(nameColumn)
            headerRow.addView(arrowView)
            headerRow.addView(deleteBtn)
            cardContent.addView(headerRow)

            // ===== 可折叠详情区 =====
            val detailSection = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = if (isExpanded) View.VISIBLE else View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // 分割线
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                ).apply { topMargin = dp(10); bottomMargin = dp(8) }
                setBackgroundColor(0xFFF5F5F5.toInt())
            }
            detailSection.addView(divider)

            // 简称标签（ChipGroup 自动换行）
            val aliasLabel = TextView(this).apply {
                text = "简称"
                textSize = 12f
                setTextColor(0xFF9E9E9E.toInt())
                setPadding(0, 0, 0, dp(4))
            }
            detailSection.addView(aliasLabel)

            val chipGroup = ChipGroup(this).apply {
                chipSpacingHorizontal = dp(4)
                chipSpacingVertical = dp(2)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            aliases.forEach { alias ->
                val chip = Chip(this).apply {
                    text = alias
                    textSize = 12f
                    isCloseIconVisible = true
                    chipBackgroundColor = ColorStateList.valueOf(0xFFE3F2FD.toInt())
                    setTextColor(0xFF1565C0.toInt())
                    closeIconTint = ColorStateList.valueOf(0xFF90CAF9.toInt())
                    chipStrokeWidth = dp(1).toFloat()
                    chipStrokeColor = ColorStateList.valueOf(0xFFBBDEFB.toInt())
                    chipCornerRadius = dp(12).toFloat()
                    chipMinHeight = dp(28).toFloat()
                    chipStartPadding = dp(6).toFloat()
                    chipEndPadding = dp(2).toFloat()
                    closeIconSize = dp(14).toFloat()
                    setOnCloseIconClickListener {
                        aliases.remove(alias)
                        if (aliases.isEmpty()) {
                            contactsMap.remove(wechatName)
                        }
                        refreshContactList()
                    }
                }
                chipGroup.addView(chip)
            }
            detailSection.addView(chipGroup)

            // 添加简称输入
            val addAliasRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                clipChildren = false
                clipToPadding = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }

            val aliasInput = EditText(this).apply {
                hint = "添加新简称…"
                textSize = 14f
                setPadding(dp(14), dp(10), dp(14), dp(10))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setBackgroundResource(R.drawable.bg_alias_input)
                setHintTextColor(0xFFBDBDBD.toInt())
                isSingleLine = true
            }

            val addAliasBtn = TextView(this).apply {
                text = "+ 添加"
                textSize = 13f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundResource(R.drawable.bg_alias_add_btn)
                setPadding(dp(16), dp(9), dp(16), dp(9))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(8) }
                setOnClickListener {
                    val newAlias = aliasInput.text.toString().trim()
                    if (newAlias.isNotEmpty() && !aliases.contains(newAlias)) {
                        aliases.add(newAlias)
                        expandedContacts.add(wechatName) // 保持展开
                        aliasInput.text.clear()
                        refreshContactList()
                    } else if (newAlias.isEmpty()) {
                        Toast.makeText(this@VoiceSettingsActivity, "请输入简称", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            addAliasRow.addView(aliasInput)
            addAliasRow.addView(addAliasBtn)
            detailSection.addView(addAliasRow)

            cardContent.addView(detailSection)

            // 点击头部切换展开/收起
            headerRow.setOnClickListener {
                if (expandedContacts.contains(wechatName)) {
                    expandedContacts.remove(wechatName)
                    arrowView.text = "▼"
                    detailSection.visibility = View.GONE
                } else {
                    expandedContacts.add(wechatName)
                    arrowView.text = "▲"
                    detailSection.visibility = View.VISIBLE
                }
            }

            card.addView(cardContent)
            contactListContainer.addView(card)
        }
    }
    
    /** 把 0xAARRGGBB 的颜色变浅 (factor 0~1, 越大越浅) */
    private fun lightenColor(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) + ((255 - (color shr 16 and 0xFF)) * factor)).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) + ((255 - (color shr 8 and 0xFF)) * factor)).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) + ((255 - (color and 0xFF)) * factor)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun refreshTagList(container: LinearLayout, list: MutableList<String>, bgColor: Int) {
        container.removeAllViews()

        val chipGroup = ChipGroup(this).apply {
            chipSpacingHorizontal = dp(6)
            chipSpacingVertical = dp(4)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        list.forEach { text ->
            val chip = Chip(this).apply {
                this.text = text
                textSize = 13f
                isCloseIconVisible = true
                chipBackgroundColor = ColorStateList.valueOf(lightenColor(bgColor, 0.82f))
                setTextColor(bgColor)
                closeIconTint = ColorStateList.valueOf(lightenColor(bgColor, 0.4f))
                chipStrokeWidth = dp(1).toFloat()
                chipStrokeColor = ColorStateList.valueOf(lightenColor(bgColor, 0.6f))
                chipCornerRadius = dp(16).toFloat()
                chipMinHeight = dp(32).toFloat()
                setOnCloseIconClickListener {
                    list.remove(text)
                    refreshTagList(container, list, bgColor)
                }
            }
            chipGroup.addView(chip)
        }

        container.addView(chipGroup)
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

