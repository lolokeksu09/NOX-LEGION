package studio.nox.app

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.nox.data.NoxPreset
import studio.nox.model.SceneCatalog
import studio.nox.model.SceneConfig
import studio.nox.model.SceneLooks
import studio.nox.model.SceneLook
import studio.nox.model.ScenePresets
import studio.nox.model.SceneSurprise
import studio.nox.model.SceneRegistry
import studio.nox.model.SceneCategory
import studio.nox.model.NamedScenePreset
import studio.nox.model.ColorHarmony
import studio.nox.model.EditHistory
import studio.nox.model.PortablePreset
import studio.nox.model.PresetTransfer
import kotlin.math.roundToInt
import studio.nox.renderer.MotionSensor
import studio.nox.renderer.NoxSurfaceView
import studio.nox.renderer.OffscreenExporter

private val Ink=Color(0xFF000000)
private val Surface=Color(0xFF101923)
private val Cyan=Color(0xFF30D9FF)
private val Muted=Color(0xFF9EAFC1)
private val TextWhite=Color(0xFFF2F6FF)
private val palettes=listOf(0x30D9FF,0x8C73FF,0xFF689E,0xE0EEFF,0x45FFCF,0xFFC177)
private enum class Tab { HOME, STUDIO, LIBRARY, SETTINGS }

@Composable fun NoxApp(onApply:()->Unit,vm:NoxViewModel=viewModel()) {
    val presets by vm.presets.collectAsState()
    val settings by vm.settings.collectAsState()
    var tab by remember { mutableStateOf(Tab.HOME) }
    var homeScene by remember { mutableStateOf("FLUX") }
    var homeDraft by remember { mutableStateOf<SceneConfig?>(null) }
    var editing by remember { mutableStateOf(SceneCatalog.default("FLUX")) }
    var history by remember { mutableStateOf(EditHistory.start(SceneCatalog.default("FLUX"))) }
    var editId by remember { mutableLongStateOf(0) }
    var editName by remember { mutableStateOf("Blue Current") }
    var pendingExport by remember { mutableStateOf<SceneConfig?>(null) }
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val active=presets.firstOrNull {it.id==settings.activeId}
    LaunchedEffect(active?.scene?.sceneId) { if(active!=null) {homeScene=active.scene.sceneId;homeDraft=null} }
    val previewHomeConfig = homeDraft?.takeIf {it.sceneId==homeScene}
        ?: if(active?.scene?.sceneId==homeScene) active.scene else SceneCatalog.default(homeScene)
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri:Uri? ->
        val snapshot=pendingExport;pendingExport=null
        if(uri==null || snapshot==null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.Default) {
                    val metrics=context.resources.displayMetrics
                    val w=metrics.widthPixels.coerceIn(1,4096)
                    val h=metrics.heightPixels.coerceIn(1,4096)
                    val bitmap=OffscreenExporter.render(snapshot,w,h)
                    try {
                        context.contentResolver.openOutputStream(uri)?.use {
                            check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) {"PNG compression failed"}
                        }?:error("Невозможно открыть выбранный файл")
                    } finally {bitmap.recycle()}
                }
            }.onSuccess {vm.notify("PNG сохранён")}.onFailure {vm.notify(it.message?:"Ошибка экспорта PNG")}
        }
    }
    val portableExportLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")) { uri:Uri? ->
        if(uri!=null) scope.launch {
            runCatching {
                val items=presets.map { PortablePreset(it.name,it.scene) }
                val data=PresetTransfer.encode(items)
                withContext(Dispatchers.IO) {
                    val output=context.contentResolver.openOutputStream(uri)
                        ?: error("Не удалось открыть файл")
                    output.bufferedWriter(Charsets.UTF_8).use { it.write(data) }
                }
            }.onSuccess {vm.notify("Коллекция экспортирована")}
             .onFailure {vm.notify(it.message?:"Не удалось сохранить коллекцию")}
        }
    }
    val portableImportLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()) { uri:Uri? ->
        if(uri!=null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val bytes=ByteArrayOutputStream()
                    val input=context.contentResolver.openInputStream(uri)
                        ?: error("Невозможно прочитать файл")
                    input.use { stream ->
                        val chunk=ByteArray(8192)
                        while(true) {
                            val count=stream.read(chunk)
                            if(count<0) break
                            bytes.write(chunk,0,count)
                            require(bytes.size()<=256_000) { "Файл слишком большой" }
                        }
                    }
                    PresetTransfer.decode(bytes.toString(Charsets.UTF_8.name()))
                }
            }.onSuccess {vm.importPortable(it)}
             .onFailure {vm.notify(it.message?:"Ошибка импорта")}
        }
    }
    fun openStudio(c:SceneConfig,id:Long=0,name:String="") {
        editing=c.normalized()
        history=EditHistory.start(editing)
        editId=id
        editName=name.ifBlank {"${c.sceneId} custom"}
        tab=Tab.STUDIO
    }
    fun editScene(c:SceneConfig) {
        editing=c.normalized()
        history=history.record(editing)
    }
    fun export(c:SceneConfig) { pendingExport=c;exportLauncher.launch("NOX-${c.sceneId.lowercase()}.png") }
    fun applySafely() { runCatching {onApply()}.onFailure {vm.notify(it.message?:"Ошибка открытия системных обоев")} }
    MaterialTheme(colorScheme=darkColorScheme(primary=Cyan,onPrimary=Ink,background=Ink,onBackground=TextWhite,surface=Surface,onSurface=TextWhite)) {
        Scaffold(containerColor=Ink, bottomBar={
            NavigationBar(containerColor=Color(0xFF080D13), tonalElevation=0.dp) {
                listOf(Tab.HOME,Tab.STUDIO,Tab.LIBRARY,Tab.SETTINGS).forEach { t ->
                    val selected = tab == t
                    NavigationBarItem(selected=selected,onClick={tab=t},icon={
                        Icon(imageVector=when(t){
                            Tab.HOME -> Icons.Outlined.Home
                            Tab.STUDIO -> Icons.Outlined.AutoAwesome
                            Tab.LIBRARY -> Icons.Outlined.CollectionsBookmark
                            Tab.SETTINGS -> Icons.Outlined.Tune
                        },contentDescription=null,modifier=Modifier.size(23.dp))
                    },label={Text(when(t){
                        Tab.HOME->"Обзор"
                        Tab.STUDIO->"Студия"
                        Tab.LIBRARY->"Коллекция"
                        Tab.SETTINGS->"Настройки"
                    },fontSize=11.sp)},alwaysShowLabel=true,
                        colors=NavigationBarItemDefaults.colors(
                            selectedIconColor=Cyan, selectedTextColor=TextWhite,
                            indicatorColor=Color(0xFF17303E), unselectedIconColor=Muted,
                            unselectedTextColor=Muted))
                }
            }
        }) { insets ->
            Column(Modifier.fillMaxSize().padding(insets).background(Ink)) {
                Row(Modifier.fillMaxWidth().padding(start=22.dp,end=22.dp,top=14.dp,bottom=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    Column {
                        Text("NOX",fontSize=31.sp,fontWeight=FontWeight.Black,color=TextWhite,letterSpacing=4.sp)
                        Text("LIVING LIGHT STUDIO",fontSize=9.sp,color=Cyan,letterSpacing=2.sp)
                    }
                    Text(BuildConfig.VERSION_NAME.removeSuffix(".0"),fontSize=12.sp,color=Muted)
                }
                when(tab) {
                    Tab.HOME -> HomeScreen(homeScene,active,homeDraft,settings.fps,settings.gyroEnabled,
                        onScene={homeScene=it;homeDraft=null},
                        onRemix={homeDraft=SceneLooks.shuffle(previewHomeConfig)},
                        onSurprise={
                            val next=SceneSurprise.choose(homeScene,System.nanoTime().toInt())
                            homeScene=next.sceneId
                            homeDraft=next
                        },
                        onLook={look->homeDraft=SceneLooks.apply(previewHomeConfig,look)},
                        onVariant={variant->homeDraft=variant.config},
                        onEdit={config->
                            val current=active?.takeIf {p->p.scene==config}
                            openStudio(config,current?.id?:0,current?.name?:"")
                        },
                        onApply={
                            if(homeDraft?.sceneId==homeScene) {
                                vm.saveAndApply("${homeScene} remix",previewHomeConfig) {applySafely()}
                            } else if(active?.scene?.sceneId==homeScene) {
                                applySafely()
                            } else {
                                vm.saveAndApply("${homeScene} default",previewHomeConfig) {applySafely()}
                            }
                        },
                        onExport={export(it)})
                    Tab.STUDIO -> StudioScreen(editing,editName,settings.fps,settings.gyroEnabled,
                        canUndo=history.canUndo,canRedo=history.canRedo,
                        onUndo={
                            val next=history.undo()
                            history=next
                            editing=next.current
                        },
                        onRedo={
                            val next=history.redo()
                            history=next
                            editing=next.current
                        },
                        onEdit={editScene(it)},
                        onLiveEdit={editing=it.normalized()},
                        onLiveCommit={history=history.record(editing)},
                        onName={editName=it},onSave={vm.save(editName,editing,editId) { editId=it }},
                        onExport={export(editing)},onApply={
                            vm.saveAndApply(editName,editing,editId,onSaved={editId=it}) {applySafely()}
                        })
                    Tab.LIBRARY -> LibraryScreen(presets,settings.activeId,
                        onEdit={openStudio(it.scene,it.id,it.name)},onUse={vm.use(it)},
                        onDuplicate={vm.duplicate(it)},onDelete={vm.delete(it)},
                        favoriteIds=settings.favoriteIds,onToggleFavorite={vm.toggleFavorite(it)})
                    Tab.SETTINGS -> SettingsScreen(settings.fps,settings.gyroEnabled,vm::setFps,vm::setGyro,
                        presetCount=presets.size,
                        onExportPresets={
                            if(presets.isEmpty()) vm.notify("Пока нет пресетов для экспорта")
                            else portableExportLauncher.launch("NOX-presets.nox")
                        },
                        onImportPresets={
                            portableImportLauncher.launch(arrayOf("*/*"))
                        })
                }
            }
            vm.message?.let { message ->
                AlertDialog(onDismissRequest={vm.notify(null)},confirmButton={TextButton(onClick={vm.notify(null)}){Text("Понятно")}},title={Text("NOX")},text={Text(message)})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun HomeScreen(
    scene:String, active:NoxPreset?, draft:SceneConfig?, fps:Int, gyro:Boolean,
    onScene:(String)->Unit, onRemix:()->Unit, onSurprise:()->Unit, onLook:(SceneLook)->Unit,
    onVariant:(NamedScenePreset)->Unit,
    onEdit:(SceneConfig)->Unit, onApply:()->Unit, onExport:(SceneConfig)->Unit
) {
    var showLooks by remember { mutableStateOf(false) }
    var showVariants by remember { mutableStateOf(false) }
    val config = draft?.takeIf { it.sceneId == scene }
        ?: if(active?.scene?.sceneId==scene) active.scene else SceneCatalog.default(scene)
    val description = when(scene) {
        "HALO" -> "Свет в бесконечном вращении"
        "NEBULA" -> "Тихое движение звёзд"
        "AURORA" -> "Северное сияние в движении"
        "PULSE" -> "Ритм электрического света"
        "COMET" -> "Тонкие следы далёких комет"
        "VEIL" -> "Шёлковая дымка в глубоком чёрном"
        "GRID" -> "Световая архитектура бесконечности"
        "SHARDS" -> "Медленно вращающиеся кристаллы"
        else -> "Линии, которые чувствуют движение"
    }
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=4.dp,bottom=14.dp),
            verticalAlignment=Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Создай свой свет.",color=TextWhite,fontSize=25.sp,
                    fontWeight=FontWeight.SemiBold,letterSpacing=(-0.6f).sp)
                Text("Твоя личная галерея движения",color=Muted,fontSize=12.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xFF0F2934))
                .padding(horizontal=10.dp,vertical=6.dp)) {
                Text(if(draft?.sceneId==scene) "REMIX" else "LIVE",
                    fontSize=10.sp,fontWeight=FontWeight.Bold,color=Cyan)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(24.dp))
            .background(Ink).border(1.dp,Color(0xFF223C4C),RoundedCornerShape(24.dp))) {
            LivePreview(config,fps,gyro,Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                0f to Color.Black.copy(alpha=.14f),
                0.52f to Color.Transparent,
                1f to Color.Black.copy(alpha=.94f))))
            Row(Modifier.align(Alignment.TopStart).padding(19.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(Cyan,CircleShape))
                Spacer(Modifier.width(9.dp))
                Text("ATELIER    /    ${scene}",fontSize=11.sp,color=TextWhite,letterSpacing=1.1.sp)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
                Text(scene,color=TextWhite,fontSize=50.sp,
                    fontWeight=FontWeight.Black,letterSpacing=(-1f).sp)
                Text(description,color=Muted,fontSize=13.sp)
            }
            Text("%02d / %02d".format(SceneCatalog.ids.indexOf(scene)+1,SceneCatalog.ids.size),color=Muted.copy(alpha=.7f),fontSize=10.sp,
                modifier=Modifier.align(Alignment.BottomEnd).padding(24.dp))
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            SceneCatalog.ids.forEach { id ->
                SceneMiniature(id=id,selected=id==scene,modifier=Modifier.width(141.dp),
                    onClick={onScene(id)})
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button(onClick={onEdit(config)},modifier=Modifier.weight(1f).height(54.dp),
                shape=RoundedCornerShape(14.dp),
                colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=Ink)) {
                Icon(Icons.Outlined.Tune,contentDescription=null,modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("В студию",fontWeight=FontWeight.Bold)
            }
            OutlinedButton(onClick=onApply,modifier=Modifier.weight(1f).height(54.dp),
                shape=RoundedCornerShape(14.dp),
                border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF365061))) {
                Text("На экран",color=TextWhite,fontWeight=FontWeight.SemiBold)
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(top=2.dp,bottom=5.dp),
            horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick=onSurprise) {
                Icon(Icons.Outlined.Shuffle,contentDescription=null,tint=Cyan,
                    modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Сюрприз",color=Cyan,fontSize=12.sp)
            }
            TextButton(onClick=onRemix) {
                Icon(Icons.Outlined.AutoAwesome,contentDescription=null,tint=Cyan,
                    modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Новый вариант",color=Cyan,fontSize=12.sp)
            }
            if(ScenePresets.forScene(scene).isNotEmpty()) {
                TextButton(onClick={showVariants=true}) {
                    Icon(Icons.Outlined.AutoFixHigh,contentDescription=null,tint=Cyan,
                        modifier=Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Варианты",color=Cyan,fontSize=12.sp)
                }
            }
            TextButton(onClick={showLooks=true}) {
                Icon(Icons.Outlined.Palette,contentDescription=null,tint=TextWhite,
                    modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Стили",color=TextWhite,fontSize=12.sp)
            }
            TextButton(onClick={onExport(config)}) {
                Icon(Icons.Outlined.FileDownload,contentDescription=null,tint=Muted,
                    modifier=Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("PNG",color=Muted,fontSize=12.sp)
            }
        }
    }
    if(showVariants) {
        ModalBottomSheet(onDismissRequest={showVariants=false},
            containerColor=Color(0xFF0B121B)) {
            Column(Modifier.fillMaxWidth().padding(horizontal=22.dp)) {
                Text("Характер света",color=TextWhite,fontSize=24.sp,
                    fontWeight=FontWeight.SemiBold)
                Text("Авторские варианты. Твой пресет не изменяется.",
                    color=Muted,fontSize=12.sp)
                Spacer(Modifier.height(15.dp))
                ScenePresets.forScene(scene).forEach { variant ->
                    Row(Modifier.fillMaxWidth().padding(bottom=11.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .clickable {onVariant(variant);showVariants=false}
                        .padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(width=102.dp,height=59.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Brush.horizontalGradient(listOf(
                                Color(0xFF000000.toInt() or variant.config.primary),
                                Color(0xFF000000.toInt() or variant.config.secondary)))))
                        Spacer(Modifier.width(15.dp))
                        Column(Modifier.weight(1f)) {
                            Text(variant.name,color=TextWhite,fontSize=16.sp,
                                fontWeight=FontWeight.SemiBold)
                            Text(scene,color=Muted,fontSize=10.sp,letterSpacing=1.sp)
                        }
                        Icon(Icons.Outlined.ArrowForwardIos,contentDescription=null,
                            tint=Muted,modifier=Modifier.size(15.dp))
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
        }
    }
    if(showLooks) {
        ModalBottomSheet(onDismissRequest={showLooks=false},
            containerColor=Color(0xFF0B121B),
            dragHandle={BottomSheetDefaults.DragHandle(color=Color(0xFF45596A))}) {
            Column(Modifier.fillMaxWidth().padding(horizontal=21.dp)) {
                Text("Коллекция света",color=TextWhite,fontWeight=FontWeight.SemiBold,
                    fontSize=23.sp)
                Spacer(Modifier.height(3.dp))
                Text("Выбери характер сцены. Исходный пресет не изменится.",
                    color=Muted,fontSize=12.sp)
                Spacer(Modifier.height(15.dp))
                SceneLooks.looks.forEach { look ->
                    Row(Modifier.fillMaxWidth().padding(bottom=9.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF121D2A))
                        .border(1.dp,Color(0xFF203749),RoundedCornerShape(17.dp))
                        .clickable {onLook(look);showLooks=false}
                        .padding(11.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(width=96.dp,height=57.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Brush.horizontalGradient(listOf(
                                Color(0xFF000000.toInt() or look.primary),
                                Color(0xFF000000.toInt() or look.secondary)))))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(look.label,color=TextWhite,fontSize=16.sp,
                                fontWeight=FontWeight.SemiBold)
                            Spacer(Modifier.height(3.dp))
                            Text("ПРИМЕНИТЬ К СЦЕНЕ",color=Muted,fontSize=10.sp,
                                letterSpacing=0.65f.sp)
                        }
                        Icon(Icons.Outlined.ArrowForwardIos,contentDescription=null,
                            tint=Muted,modifier=Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable private fun SceneMiniature(
    id:String,selected:Boolean,modifier:Modifier=Modifier,onClick:()->Unit
) {
    Row(modifier.height(67.dp).clip(RoundedCornerShape(14.dp))
        .background(if(selected) Color(0xFF15303F) else Color(0xFF0D141C))
        .border(1.dp,if(selected) Cyan.copy(alpha=.65f) else Color(0xFF243644),
            RoundedCornerShape(14.dp))
        .clickable(onClick=onClick).padding(8.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(37.dp).clip(RoundedCornerShape(9.dp)).background(Ink)) {
            Miniature(SceneCatalog.default(id),Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(7.dp))
        Column {
            Text(id,fontSize=10.sp,fontWeight=FontWeight.SemiBold,
                color=if(selected) Cyan else TextWhite,maxLines=1)
            if(SceneRegistry.get(id)?.isNew==true) {
                Spacer(Modifier.height(4.dp))
                Text("NEW",fontSize=9.sp,color=Cyan,fontWeight=FontWeight.Bold,
                    letterSpacing=1.sp)
            }
        }
    }
}

@Composable private fun StudioScreen(
    config:SceneConfig, name:String, fps:Int, gyro:Boolean,
    canUndo:Boolean, canRedo:Boolean,
    onUndo:()->Unit, onRedo:()->Unit,
    onEdit:(SceneConfig)->Unit,
    onLiveEdit:(SceneConfig)->Unit, onLiveCommit:()->Unit,
    onName:(String)->Unit, onSave:()->Unit,
    onExport:()->Unit, onApply:()->Unit
) {
    var immersive by remember { mutableStateOf(false) }
    var compareOriginal by remember { mutableStateOf(false) }
    var compactPreview by remember { mutableStateOf(false) }
    val displayConfig = if (compareOriginal) SceneCatalog.default(config.sceneId) else config
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val previewHeight = if (compactPreview)
            (maxHeight * 0.25f).coerceIn(122.dp, 192.dp)
        else (maxHeight * 0.39f).coerceIn(144.dp, 315.dp)
        Column(Modifier.fillMaxSize().padding(horizontal=18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=2.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Студия света",color=TextWhite,fontSize=27.sp,fontWeight=FontWeight.SemiBold)
                Text("Точность света. Свобода цвета.",color=Muted,fontSize=12.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF102D3B))
                .padding(horizontal=13.dp,vertical=7.dp)) {
                Text("SIGNATURE",color=Cyan,fontSize=10.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        Box(Modifier.fillMaxWidth().height(previewHeight).clip(RoundedCornerShape(22.dp))
            .border(1.dp,Color(0xFF233748),RoundedCornerShape(22.dp))) {
            if(!immersive) LivePreview(displayConfig,fps,gyro,Modifier.fillMaxSize())
            Row(Modifier.align(Alignment.TopStart).padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Cyan,CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(config.sceneId, fontSize=11.sp,color=TextWhite,letterSpacing=1.5.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xD9000000))))
                .padding(start=10.dp,end=10.dp,bottom=8.dp,top=14.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={compareOriginal=!compareOriginal},
                    colors=ButtonDefaults.textButtonColors(contentColor=if(compareOriginal) Cyan else TextWhite)) {
                    Icon(Icons.Outlined.CompareArrows,contentDescription=null,
                        modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if(compareOriginal) "Моя сцена" else "Оригинал",fontSize=12.sp)
                }
                Row(verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={compactPreview=!compactPreview}) {
                        Icon(if(compactPreview) Icons.Outlined.Expand else Icons.Outlined.Compress,
                            contentDescription=if(compactPreview) "Увеличить превью" else "Уменьшить превью",
                            tint=TextWhite)
                    }
                    IconButton(onClick={immersive=true},
                        modifier=Modifier.background(Color(0xC11B2938),CircleShape)) {
                        Icon(Icons.Outlined.Fullscreen,contentDescription="На весь экран",tint=TextWhite)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF101924))
            .border(1.dp,Color(0xFF203746),RoundedCornerShape(15.dp))
            .padding(horizontal=8.dp,vertical=5.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text("ИСТОРИЯ",fontSize=10.sp,color=Muted,letterSpacing=1.2.sp,
                modifier=Modifier.weight(1f).padding(start=10.dp))
            IconButton(onClick=onUndo,enabled=canUndo) {
                Icon(Icons.Outlined.Undo,contentDescription="Отменить",tint=if(canUndo) Cyan else Color(0xFF465460))
            }
            IconButton(onClick=onRedo,enabled=canRedo) {
                Icon(Icons.Outlined.Redo,contentDescription="Повторить",tint=if(canRedo) Cyan else Color(0xFF465460))
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick={onEdit(SceneLooks.shuffle(config))},modifier=Modifier.weight(1f),
                shape=RoundedCornerShape(13.dp)) {
                Icon(Icons.Outlined.Casino,contentDescription=null,modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("Новый вариант",fontSize=12.sp)
            }
            OutlinedButton(onClick={onEdit(SceneCatalog.default(config.sceneId))},
                modifier=Modifier.weight(1f),shape=RoundedCornerShape(13.dp)) {
                Icon(Icons.Outlined.RestartAlt,contentDescription=null,modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(6.dp))
                Text("Сбросить",fontSize=12.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Цветовые коллекции",fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=TextWhite)
        Text("Авторские сочетания для каждой сцены",fontSize=12.sp,color=Muted)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            SceneLooks.looks.forEach { look ->
                Column(Modifier.width(112.dp).clip(RoundedCornerShape(14.dp))
                    .background(Surface).clickable { onEdit(SceneLooks.apply(config, look)) }.padding(9.dp)) {
                    Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(9.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF000000),Color(0xFF000000.toInt() or look.secondary),
                            Color(0xFF000000.toInt() or look.primary)))))
                    Spacer(Modifier.height(8.dp))
                    Text(look.label,color=TextWhite,fontSize=12.sp,maxLines=1)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.Palette,contentDescription=null,tint=Cyan,modifier=Modifier.size(21.dp))
            Spacer(Modifier.width(9.dp))
            Text("Цветовые гармонии",fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=TextWhite)
        }
        Spacer(Modifier.height(5.dp))
        Text("Создай сочетание из своего основного цвета",fontSize=12.sp,color=Muted)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ColorHarmony.Mode.entries.forEach { mode ->
                val candidate = ColorHarmony.apply(config, mode)
                Column(Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                    .background(Surface).border(1.dp,Color(0xFF263746),RoundedCornerShape(15.dp))
                    .clickable { onEdit(ColorHarmony.apply(config, mode)) }
                    .padding(9.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(37.dp).clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(
                            Color(0xFF000000.toInt() or config.primary),
                            Color(0xFF000000.toInt() or candidate.secondary)))))
                    Spacer(Modifier.height(7.dp))
                    Text(mode.title,color=TextWhite,fontSize=12.sp,maxLines=1)
                }
            }
        }
        Text("Меняется только второй цвет — остальная сцена остаётся прежней",
            fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=8.dp))
        Spacer(Modifier.height(25.dp))
        Text("Свой стиль",fontSize=18.sp,fontWeight=FontWeight.SemiBold,color=TextWhite)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(name,onName,label={Text("Название пресета")},
            singleLine=true,modifier=Modifier.fillMaxWidth(),
            isError=name.trim().isEmpty()||name.trim().length>48,
            shape=RoundedCornerShape(14.dp))
        Spacer(Modifier.height(17.dp))
        Text("Основной цвет",color=TextWhite,fontSize=14.sp)
        PaletteChoices(config.primary) { onEdit(config.copy(primary=it)) }
        HexColorField(config.primary) { onEdit(config.copy(primary=it)) }
        Spacer(Modifier.height(18.dp))
        Text("Дополнительный цвет",color=TextWhite,fontSize=14.sp)
        PaletteChoices(config.secondary) { onEdit(config.copy(secondary=it)) }
        HexColorField(config.secondary) { onEdit(config.copy(secondary=it)) }
        Spacer(Modifier.height(20.dp))
        Text("Точная настройка",fontSize=19.sp,fontWeight=FontWeight.SemiBold,color=TextWhite)
        Spacer(Modifier.height(4.dp))
        Text("±1% кнопками или плавная настройка ползунком",fontSize=12.sp,color=Muted)
        Spacer(Modifier.height(14.dp))
        Parameter("Скорость",config.speed,
            onValueChange={onLiveEdit(config.copy(speed=it))},
            onValueCommit=onLiveCommit,
            onStep={onEdit(config.copy(speed=it))})
        Parameter("Свечение",config.glow,
            onValueChange={onLiveEdit(config.copy(glow=it))},
            onValueCommit=onLiveCommit,
            onStep={onEdit(config.copy(glow=it))})
        Parameter("Плотность",config.density,
            onValueChange={onLiveEdit(config.copy(density=it))},
            onValueCommit=onLiveCommit,
            onStep={onEdit(config.copy(density=it))})
        Parameter("Параллакс",config.motion,
            onValueChange={onLiveEdit(config.copy(motion=it))},
            onValueCommit=onLiveCommit,
            onStep={onEdit(config.copy(motion=it))})
        Spacer(Modifier.height(24.dp))
        }
        SaveActionDock(
            canSave=name.trim().isNotBlank() && name.trim().length<=48,
            onSave=onSave,onExport=onExport,onApply=onApply
        )
        }
    }
    if(immersive) FullscreenPreview(config,fps,gyro,onClose={immersive=false})
}


@Composable private fun SaveActionDock(
    canSave:Boolean,onSave:()->Unit,onExport:()->Unit,onApply:()->Unit
) {
    Row(Modifier.fillMaxWidth().padding(top=10.dp,bottom=7.dp),
        horizontalArrangement=Arrangement.spacedBy(9.dp),
        verticalAlignment=Alignment.CenterVertically) {
        Button(onClick=onSave,enabled=canSave,
            modifier=Modifier.weight(1f).height(52.dp),
            shape=RoundedCornerShape(14.dp),
            colors=ButtonDefaults.buttonColors(containerColor=Cyan,contentColor=Ink)) {
            Icon(Icons.Outlined.Save,contentDescription=null,modifier=Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Сохранить",fontWeight=FontWeight.Bold,fontSize=13.sp)
        }
        OutlinedIconButton(onClick=onExport,modifier=Modifier.size(52.dp),
            border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF304452)),
            shape=RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.FileDownload,contentDescription="Экспорт PNG",tint=TextWhite)
        }
        OutlinedIconButton(onClick=onApply,modifier=Modifier.size(52.dp),
            border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF304452)),
            shape=RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.Wallpaper,contentDescription="Установить обои",tint=TextWhite)
        }
    }
}

@Composable private fun FullscreenPreview(config:SceneConfig,fps:Int,gyro:Boolean,onClose:()->Unit) {
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            LivePreview(config,fps,gyro,Modifier.fillMaxSize())
            Box(Modifier.fillMaxWidth().height(135.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent,Ink))))
            Row(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(24.dp),
                horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text("NOX / "+config.sceneId,color=TextWhite,fontSize=13.sp,letterSpacing=2.sp)
                IconButton(onClick=onClose,modifier=Modifier.background(Color(0xC51A2635),CircleShape)) {
                    Icon(Icons.Outlined.Close,contentDescription="Закрыть",tint=TextWhite)
                }
            }
            Text("Прикоснись к свету",modifier=Modifier.align(Alignment.BottomCenter).padding(28.dp),
                color=Muted,fontSize=13.sp)
        }
    }
}

@Composable private fun HexColorField(selected:Int,onSelect:(Int)->Unit) {
    var value by remember(selected) { mutableStateOf(selected.toString(16).padStart(6,'0').uppercase()) }
    val valid = SceneCatalog.fromHex(value)!=null
    OutlinedTextField(value=value,onValueChange={ typed ->
        val next=typed.removePrefix("#").take(6).uppercase()
        if(next.all { it in '0'..'9' || it in 'A'..'F' }) {
            value=next
            SceneCatalog.fromHex(next)?.let(onSelect)
        }
    },label={Text("HEX · RRGGBB")},prefix={Text("#")},singleLine=true,
        isError=!valid,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp))
}

@Composable private fun PaletteChoices(selected:Int,onSelect:(Int)->Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=11.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        palettes.forEach { color ->
            Box(Modifier.size(41.dp).border(if(selected==color)3.dp else 1.dp,if(selected==color)TextWhite else Color.DarkGray,CircleShape).padding(5.dp)
                .background(Color(0xFF000000.toInt() or color),CircleShape).clip(CircleShape).clickable {onSelect(color)})
        }
    }
}

@Composable private fun Parameter(
    name:String, value:Float,
    onValueChange:(Float)->Unit,
    onValueCommit:()->Unit,
    onStep:(Float)->Unit
) {
    val percentage=(value*100f).roundToInt().coerceIn(0,100)
    Column(Modifier.fillMaxWidth().padding(bottom=11.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF0D151E))
        .border(1.dp,Color(0xFF1C303F),RoundedCornerShape(16.dp))
        .padding(horizontal=15.dp,vertical=13.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween) {
            Text(name,color=TextWhite,fontSize=14.sp,fontWeight=FontWeight.Medium)
            Text("${percentage}%",color=Cyan,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            IconButton(onClick={onStep(((percentage-1).coerceAtLeast(0))/100f)},
                enabled=percentage>0,modifier=Modifier.size(46.dp)
                    .background(Color(0xFF192938),RoundedCornerShape(13.dp))) {
                Icon(Icons.Outlined.Remove,contentDescription="Уменьшить ${name} на 1%",tint=if(percentage>0) TextWhite else Muted)
            }
            Slider(value=value.coerceIn(0f,1f),
                onValueChange=onValueChange,
                onValueChangeFinished=onValueCommit,
                valueRange=0f..1f,
                modifier=Modifier.weight(1f),
                colors=SliderDefaults.colors(
                    thumbColor=Cyan,activeTrackColor=Cyan,
                    inactiveTrackColor=Color(0xFF334358)
                ))
            IconButton(onClick={onStep(((percentage+1).coerceAtMost(100))/100f)},
                enabled=percentage<100,modifier=Modifier.size(46.dp)
                    .background(Color(0xFF192938),RoundedCornerShape(13.dp))) {
                Icon(Icons.Outlined.Add,contentDescription="Увеличить ${name} на 1%",tint=if(percentage<100) TextWhite else Muted)
            }
        }
    }
}

@Composable private fun LibraryScreen(presets:List<NoxPreset>,active:Long?,onEdit:(NoxPreset)->Unit,onUse:(NoxPreset)->Unit,onDuplicate:(NoxPreset)->Unit,onDelete:(NoxPreset)->Unit,favoriteIds:Set<Long>,onToggleFavorite:(NoxPreset)->Unit) {
    SearchableLibrary(presets,active,onEdit,onUse,onDuplicate,onDelete,favoriteIds,onToggleFavorite)
}

@Composable private fun SearchableLibrary(
    presets:List<NoxPreset>,active:Long?,
    onEdit:(NoxPreset)->Unit,onUse:(NoxPreset)->Unit,
    onDuplicate:(NoxPreset)->Unit,onDelete:(NoxPreset)->Unit,
    favoriteIds:Set<Long>,onToggleFavorite:(NoxPreset)->Unit
) {
    var confirming by remember { mutableStateOf<NoxPreset?>(null) }
    var query by remember { mutableStateOf("") }
    var filterScene by remember { mutableStateOf("ALL") }
    var filterCategory by remember { mutableStateOf<SceneCategory?>(null) }
    var filterFavorites by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("Недавние") }
    val filtered=remember(presets,query,filterScene,filterCategory,sortOrder,filterFavorites,favoriteIds) {
        val matches=presets.filter { preset ->
            (filterScene=="ALL" || preset.scene.sceneId==filterScene) &&
            (filterCategory==null || SceneRegistry.get(preset.scene.sceneId)?.category==filterCategory) &&
            (!filterFavorites || preset.id in favoriteIds) &&
            (preset.name.contains(query.trim(),ignoreCase=true) ||
                preset.scene.sceneId.contains(query.trim(),ignoreCase=true))
        }
        when(sortOrder) {
            "По имени" -> matches.sortedBy { it.name.lowercase() }
            "По сцене" -> matches.sortedWith(compareBy({it.scene.sceneId},{it.name.lowercase()}))
            else -> matches.sortedByDescending {it.id}
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top=4.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Коллекция",fontSize=27.sp,fontWeight=FontWeight.SemiBold,color=TextWhite)
                Text("Твои уникальные композиции",color=Muted,fontSize=12.sp)
            }
            Row(Modifier.clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF102632)).padding(horizontal=11.dp,vertical=7.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(Cyan,CircleShape))
                Spacer(Modifier.width(7.dp))
                Text(filtered.size.toString()+" работ",fontSize=11.sp,
                    color=Cyan,fontWeight=FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(11.dp))
        OutlinedTextField(
            value=query,onValueChange={query=it},singleLine=true,
            placeholder={Text("Найти композицию",color=Muted,fontSize=13.sp)},
            leadingIcon={Icon(Icons.Outlined.Search,contentDescription=null,tint=Muted,
                modifier=Modifier.size(20.dp))},
            modifier=Modifier.fillMaxWidth().height(52.dp),
            shape=RoundedCornerShape(13.dp),
            textStyle=androidx.compose.ui.text.TextStyle(fontSize=14.sp,color=TextWhite)
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            FilterChip(selected=filterFavorites,onClick={filterFavorites=!filterFavorites},
                modifier=Modifier.height(39.dp),
                label={
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Star,contentDescription=null,
                            modifier=Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Избранное",fontSize=11.sp)
                    }
                },
                shape=RoundedCornerShape(11.dp),
                colors=FilterChipDefaults.filterChipColors(
                    selectedContainerColor=Color(0xFF164052),
                    selectedLabelColor=Cyan,containerColor=Color(0xFF0D141C),
                    labelColor=Muted))
            Spacer(Modifier.width(3.dp))
            (listOf("ALL")+SceneCatalog.ids).forEach { key ->
                FilterChip(selected=filterScene==key,onClick={filterScene=key;filterCategory=null},
                    modifier=Modifier.height(39.dp),
                    label={Text(if(key=="ALL") "Все" else key,fontSize=11.sp)},
                    shape=RoundedCornerShape(11.dp),
                    colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=Color(0xFF163747),
                        selectedLabelColor=Cyan,containerColor=Color(0xFF0D141C),
                        labelColor=Muted))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            val categories=listOf(
                "Все категории" to null,
                "Signature" to SceneCategory.SIGNATURE,
                "Ambient" to SceneCategory.AMBIENT,
                "Tech" to SceneCategory.TECH,
                "Cosmic" to SceneCategory.COSMIC
            )
            categories.forEach { (label,category) ->
                FilterChip(selected=filterCategory==category,
                    onClick={filterCategory=category;filterScene="ALL"},
                    modifier=Modifier.height(36.dp),
                    label={Text(label,fontSize=11.sp)},
                    shape=RoundedCornerShape(11.dp),
                    colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=Color(0xFF163747),
                        selectedLabelColor=Cyan,
                        containerColor=Color(0xFF0D141C),labelColor=Muted))
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.End) {
            Box {
                var sortMenu by remember { mutableStateOf(false) }
                TextButton(onClick={sortMenu=true},contentPadding=PaddingValues(horizontal=9.dp,vertical=0.dp)) {
                    Icon(Icons.Outlined.Sort,contentDescription=null,modifier=Modifier.size(17.dp),
                        tint=Muted)
                    Spacer(Modifier.width(7.dp))
                    Text(sortOrder,color=Muted,fontSize=11.sp)
                    Icon(Icons.Outlined.KeyboardArrowDown,contentDescription=null,
                        modifier=Modifier.size(17.dp),tint=Muted)
                }
                DropdownMenu(expanded=sortMenu,onDismissRequest={sortMenu=false}) {
                    listOf("Недавние","По имени","По сцене").forEach { option ->
                        DropdownMenuItem(text={Text(option)},onClick={
                            sortOrder=option;sortMenu=false
                        })
                    }
                }
            }
        }
        if(filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CollectionsBookmark,contentDescription=null,
                        tint=Muted,modifier=Modifier.size(40.dp))
                    Spacer(Modifier.height(13.dp))
                    Text(if(presets.isEmpty()) "Сохрани первую композицию в Студии"
                         else "В этой подборке пока ничего нет",
                        color=Muted,fontSize=14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns=GridCells.Fixed(2),
                modifier=Modifier.weight(1f),
                horizontalArrangement=Arrangement.spacedBy(12.dp),
                verticalArrangement=Arrangement.spacedBy(15.dp),
                contentPadding=PaddingValues(bottom=22.dp)
            ) {
                items(count=filtered.size,
                    key={filtered[it].id},
                    span={index ->
                        if(filtered.size % 2 == 1 && index == filtered.lastIndex)
                            GridItemSpan(2) else GridItemSpan(1)
                    }
                ) { index ->
                    val preset=filtered[index]
                    val fullWidth=filtered.size % 2 == 1 && index == filtered.lastIndex
                    var menuExpanded by remember { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF0E1823))
                        .border(1.dp,if(preset.id==active) Color(0xFF29809B)
                            else Color(0xFF213444),RoundedCornerShape(17.dp))) {
                        Box(Modifier.fillMaxWidth().height(if(fullWidth) 228.dp else 205.dp)
                            .clip(RoundedCornerShape(topStart=16.dp,topEnd=16.dp))
                            .background(Ink).clickable {onEdit(preset)}) {
                            Miniature(preset.scene,Modifier.fillMaxSize())
                            Box(Modifier.fillMaxWidth().height(65.dp)
                                .align(Alignment.BottomCenter)
                                .background(Brush.verticalGradient(
                                    listOf(Color.Transparent,Ink.copy(alpha=.88f)))))
                            if(preset.id==active) {
                                Row(Modifier.align(Alignment.TopStart).padding(10.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xD20C2934))
                                    .padding(horizontal=8.dp,vertical=5.dp),
                                    verticalAlignment=Alignment.CenterVertically) {
                                    Box(Modifier.size(5.dp).background(Cyan,CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Активно",color=Cyan,fontSize=10.sp)
                                }
                            }
                            Box(Modifier.align(Alignment.TopEnd).padding(7.dp)) {
                                IconButton(onClick={menuExpanded=true},
                                    modifier=Modifier.size(44.dp)
                                        .background(Color(0x8D101A24),CircleShape)) {
                                    Icon(Icons.Outlined.MoreHoriz,
                                        contentDescription="Действия с пресетом",
                                        tint=TextWhite,modifier=Modifier.size(21.dp))
                                }
                                DropdownMenu(expanded=menuExpanded,
                                    onDismissRequest={menuExpanded=false},
                                    containerColor=Color(0xFF15212E)) {
                                    DropdownMenuItem(text={Text("Открыть в Студии")},
                                        onClick={menuExpanded=false;onEdit(preset)},
                                        leadingIcon={Icon(Icons.Outlined.Tune,null)})
                                    DropdownMenuItem(text={Text("Сделать активным")},
                                        onClick={menuExpanded=false;onUse(preset)},
                                        leadingIcon={Icon(Icons.Outlined.CheckCircle,null)})
                                    DropdownMenuItem(text={Text("Создать копию")},
                                        onClick={menuExpanded=false;onDuplicate(preset)},
                                        leadingIcon={Icon(Icons.Outlined.ContentCopy,null)})
                                    HorizontalDivider(color=Color(0xFF2C3B48))
                                    DropdownMenuItem(text={Text("Удалить",color=Color(0xFFFF8494))},
                                        onClick={menuExpanded=false;confirming=preset},
                                        leadingIcon={Icon(Icons.Outlined.DeleteOutline,null,
                                            tint=Color(0xFFFF8494))})
                                }
                            }
                            IconButton(onClick={onToggleFavorite(preset)},
                                modifier=Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                    .size(44.dp)
                                    .background(Color(0xA5081420),CircleShape)) {
                                Icon(
                                    if(preset.id in favoriteIds) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription=if(preset.id in favoriteIds) "Убрать из избранного" else "В избранное",
                                    tint=if(preset.id in favoriteIds) Cyan else TextWhite,
                                    modifier=Modifier.size(23.dp)
                                )
                            }
                            Text(preset.scene.sceneId,color=Cyan,fontSize=10.sp,
                                letterSpacing=1.sp, fontWeight=FontWeight.Medium,
                                modifier=Modifier.align(Alignment.BottomStart).padding(13.dp))
                        }
                        Column(Modifier.fillMaxWidth().clickable{onEdit(preset)}
                            .padding(horizontal=12.dp,vertical=10.dp)) {
                            Text(preset.name,color=TextWhite,fontSize=14.sp,
                                fontWeight=FontWeight.SemiBold,maxLines=1,
                                overflow=TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            Text(if(preset.id==active) "На рабочем столе" else "Открыть в Студии",
                                color=Muted,fontSize=10.sp)
                        }
                    }
                }
            }
        }
    }
    confirming?.let { preset ->
        AlertDialog(onDismissRequest={confirming=null},title={Text("Удалить пресет?")},
            text={Text("«"+preset.name+"» будет удалён без возможности восстановления.")},
            confirmButton={TextButton(onClick={onDelete(preset);confirming=null}){Text("Удалить")}},
            dismissButton={TextButton(onClick={confirming=null}){Text("Отмена")}})
    }
}

@Composable private fun Miniature(config:SceneConfig,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val a=Color(0xFF000000.toInt() or config.primary)
        val b=Color(0xFF000000.toInt() or config.secondary)
        when(config.sceneId) {
            "HALO" -> {
                drawCircle(a.copy(alpha=.16f),radius=size.minDimension*.37f,
                    style=Stroke(width=15f))
                drawCircle(a.copy(alpha=.9f),radius=size.minDimension*.35f,
                    style=Stroke(width=2.5f))
                drawCircle(b.copy(alpha=.75f),radius=size.minDimension*.22f,
                    style=Stroke(width=1.5f))
                drawCircle(b.copy(alpha=.20f),radius=size.minDimension*.18f)
            }
            "AURORA" -> {
                for(i in 0 until 5) {
                    val n=i.toFloat()
                    val path=Path().apply {
                        moveTo(size.width*(.18f+n*.11f),size.height*1.08f)
                        cubicTo(size.width*(.46f+n*.09f),size.height*.73f,
                            size.width*(-.05f+n*.14f),size.height*.25f,
                            size.width*(.23f+n*.12f),-size.height*.1f)
                    }
                    drawPath(path,if(i%2==0)a.copy(alpha=.12f) else b.copy(alpha=.10f),
                        style=Stroke(width=size.minDimension*.21f))
                    drawPath(path,if(i%2==0)a else b,
                        style=Stroke(width=size.minDimension*.016f),alpha=.74f)
                }
            }
            "PULSE" -> {
                val centre=Offset(size.width*.50f,size.height*.52f)
                for(i in 0 until 5) {
                    val radius=size.minDimension*(.11f+i*.075f)
                    drawCircle(if(i%2==0)a else b,radius=radius,
                        center=centre,alpha=.13f,
                        style=Stroke(width=size.minDimension*.045f))
                    drawCircle(if(i%2==0)a else b,radius=radius,
                        center=centre,alpha=.83f,
                        style=Stroke(width=size.minDimension*.009f))
                }
            }
            "COMET" -> {
                for(i in 0 until 5) {
                    val x=size.width*(.17f+i*.19f)
                    val y=size.height*(.17f+(i*3%5)*.15f)
                    val start=Offset(x-size.width*.23f,y+size.height*.12f)
                    val end=Offset(x,y)
                    drawLine(if(i%2==0)a else b, start,end,
                        strokeWidth=size.minDimension*.026f,alpha=.16f)
                    drawLine(if(i%2==0)a else b,start,end,
                        strokeWidth=size.minDimension*.009f,alpha=.77f)
                    drawCircle(if(i%2==0)a else b,radius=size.minDimension*.02f,
                        center=end,alpha=.92f)
                }
            }
            "VEIL" -> {
                for(i in 0 until 5) {
                    val n=i.toFloat()
                    val y=size.height*(.22f+n*.14f)
                    val path=Path().apply {
                        moveTo(-size.width*.12f,y)
                        cubicTo(size.width*.34f,y-size.height*.13f,
                            size.width*.60f,y+size.height*.18f,
                            size.width*1.12f,y-size.height*.035f)
                    }
                    drawPath(path,if(i%2==0)a else b,
                        style=Stroke(width=size.minDimension*(.10f+n*.008f)),alpha=.13f)
                    drawPath(path,if(i%2==0)a else b,
                        style=Stroke(width=size.minDimension*.012f),alpha=.58f)
                }
            }
            "GRID" -> {
                val horizon=size.height*.37f
                for(i in -4..4) {
                    val start=Offset(size.width*.5f+i*size.width*.22f,size.height*1.1f)
                    val end=Offset(size.width*.5f+i*size.width*.025f,horizon)
                    drawLine(if(i%2==0)a else b,start,end,
                        strokeWidth=size.minDimension*.009f,alpha=.72f)
                }
                for(i in 0 until 6) {
                    val t=i/6f
                    val y=horizon+(size.height-horizon)*t*t
                    drawLine(if(i%2==0)a else b,Offset(0f,y),Offset(size.width,y),
                        strokeWidth=size.minDimension*.011f,alpha=.55f)
                }
            }
            "SHARDS" -> {
                for(i in 0 until 6) {
                    val x=size.width*(.20f+(i*3%7)*.105f)
                    val y=size.height*(.19f+(i*5%7)*.105f)
                    val radius=size.minDimension*(.075f+(i%3)*.019f)
                    val polygon=Path().apply {
                        moveTo(x,y-radius)
                        lineTo(x+radius*.72f,y)
                        lineTo(x,y+radius)
                        lineTo(x-radius*.72f,y)
                        close()
                    }
                    val color=if(i%2==0)a else b
                    drawPath(polygon,color.copy(alpha=.15f))
                    drawPath(polygon,color,style=Stroke(width=size.minDimension*.016f),alpha=.78f)
                    drawLine(color.copy(alpha=.68f),
                        Offset(x,y-radius),Offset(x,y+radius),
                        strokeWidth=size.minDimension*.006f)
                }
            }
            "NEBULA" -> {
                for(i in 0 until 29) {
                    val fx=((i*37+config.seed%97+97)%97)/97f
                    val fy=((i*59+config.seed%83+83)%83)/83f
                    val center=Offset(size.width*fx,size.height*fy)
                    drawCircle(if(i%2==0)a else b, radius=if(i%5==0)3.4f else 1.8f,
                        center=center,alpha=.7f)
                    if(i%7==0)drawCircle(a.copy(alpha=.12f),radius=12f,center=center)
                }
            }
            else -> {
                for (i in 0..3) {
                    val path=Path().apply {
                        moveTo(-size.width*.18f,size.height*(.75f-i*.07f))
                        cubicTo(size.width*.7f,size.height*(.85f-i*.22f),
                            size.width*.1f,size.height*.12f,
                            size.width*1.2f,size.height*(.15f+i*.04f))
                    }
                    drawPath(path,color=a.copy(alpha=.12f),style=Stroke(width=13f))
                    drawPath(path,color=if(i%2==0)a else b,style=Stroke(width=1.8f))
                }
            }
        }
    }
}

@Composable private fun SettingsScreen(fps:Int,gyro:Boolean,onFps:(Int)->Unit,onGyro:(Boolean)->Unit, presetCount:Int, onExportPresets:()->Unit,onImportPresets:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal=20.dp)) {
        Text("Устройство и свет",fontSize=27.sp,fontWeight=FontWeight.SemiBold,color=TextWhite)
        Text("Тонкая настройка графического движка",color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(27.dp))

        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.Speed,contentDescription=null,tint=Cyan,modifier=Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text("Производительность",fontSize=19.sp,color=TextWhite,
                fontWeight=FontWeight.SemiBold)
        }
        Spacer(Modifier.height(7.dp))
        Text("Ограничение частоты кадров для живого света",color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            listOf(30,60,120).forEach { cap ->
                val selected=fps==cap
                val mode=when(cap) {
                    30->"Экономия"
                    60->"Баланс"
                    else->"Максимум"
                }
                Column(Modifier.weight(1f).height(93.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if(selected) Color(0xFF143543) else Color(0xFF101923))
                    .border(1.dp,if(selected) Cyan.copy(alpha=.76f)
                        else Color(0xFF233747),RoundedCornerShape(15.dp))
                    .clickable {onFps(cap)},
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.Center) {
                    Text(cap.toString(),color=if(selected) Cyan else TextWhite,
                        fontWeight=FontWeight.Bold,fontSize=25.sp)
                    Text("FPS",color=Muted,fontSize=9.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(mode,color=if(selected) TextWhite else Muted,fontSize=10.sp,
                        maxLines=1)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("При нагреве и экономии энергии система может снижать фактический FPS.",
            color=Muted,fontSize=12.sp)

        Spacer(Modifier.height(27.dp))
        HorizontalDivider(color=Color(0xFF253541))
        Spacer(Modifier.height(23.dp))

        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.Explore,contentDescription=null,tint=Cyan,modifier=Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Интерактивность",fontSize=19.sp,color=TextWhite,
                fontWeight=FontWeight.SemiBold)
        }
        Spacer(Modifier.height(13.dp))
        Row(Modifier.fillMaxWidth().heightIn(min=91.dp)
            .clip(RoundedCornerShape(15.dp)).background(Color(0xFF101923))
            .border(1.dp,Color(0xFF233747),RoundedCornerShape(15.dp))
            .padding(horizontal=16.dp,vertical=11.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Реакция на движение",color=TextWhite,fontSize=15.sp,
                    fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Свет реагирует на наклон устройства",
                    fontSize=12.sp,color=Muted)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).background(if(gyro) Cyan else Muted,CircleShape))
                    Spacer(Modifier.width(7.dp))
                    Text(if(gyro) "Включено" else "Выключено",
                        fontSize=11.sp,color=if(gyro) Cyan else Muted)
                }
            }
            Switch(checked=gyro,onCheckedChange=onGyro,
                colors=SwitchDefaults.colors(checkedThumbColor=Ink,checkedTrackColor=Cyan))
        }

        Spacer(Modifier.height(27.dp))
        HorizontalDivider(color=Color(0xFF253541))
        Spacer(Modifier.height(23.dp))

        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield,contentDescription=null,
                tint=Cyan,modifier=Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Конфиденциальность и движок",color=TextWhite,
                fontSize=18.sp,fontWeight=FontWeight.SemiBold)
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFF0D151D))
            .border(1.dp,Color(0xFF203443),RoundedCornerShape(15.dp))
            .padding(horizontal=16.dp,vertical=8.dp)) {
            listOf(
                Triple(Icons.Outlined.PersonOff,"Без аккаунта","Не нужна регистрация"),
                Triple(Icons.Outlined.WifiOff,"Без интернета","Работает автономно"),
                Triple(Icons.Outlined.Folder,"Локальное хранение","Пресеты на устройстве")
            ).forEachIndexed { index,item ->
                Row(Modifier.fillMaxWidth().heightIn(min=53.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Icon(item.first,contentDescription=null,tint=Cyan.copy(alpha=.82f),
                        modifier=Modifier.size(18.dp))
                    Spacer(Modifier.width(13.dp))
                    Text(item.second,color=TextWhite,fontSize=13.sp,
                        modifier=Modifier.weight(1f))
                    Text(item.third,color=Muted,fontSize=10.sp)
                }
                if(index<2) HorizontalDivider(color=Color(0xFF253541))
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Outlined.FolderShared,contentDescription=null,tint=Cyan,
                modifier=Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text("Перенос коллекции",fontSize=18.sp,fontWeight=FontWeight.SemiBold,
                color=TextWhite)
        }
        Spacer(Modifier.height(8.dp))
        Text("Переноси пресеты между установками NOX через локальный файл. "+
            "Импорт добавляет сцены, не удаляя существующие.",
            color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(11.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick=onExportPresets,modifier=Modifier.weight(1f).height(50.dp),
                shape=RoundedCornerShape(13.dp),
                enabled=presetCount>0) {
                Icon(Icons.Outlined.FileUpload,contentDescription=null,modifier=Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Экспорт",fontSize=12.sp)
            }
            OutlinedButton(onClick=onImportPresets,modifier=Modifier.weight(1f).height(50.dp),
                shape=RoundedCornerShape(13.dp)) {
                Icon(Icons.Outlined.FileDownload,contentDescription=null,modifier=Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Импорт",fontSize=12.sp)
            }
        }
        Text("В библиотеке: ${presetCount} · файл NOX-PRESETS/1",
            color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=8.dp))
        Spacer(Modifier.height(21.dp))
        HorizontalDivider(color=Color(0xFF253541))
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
            verticalAlignment=Alignment.CenterVertically) {
            Text("NOX ${BuildConfig.VERSION_NAME} Curated",color=TextWhite,fontSize=12.sp,
                fontWeight=FontWeight.Medium)
            Text("OpenGL ES live engine",color=Muted,fontSize=11.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text("Локальная генерация световых сцен",fontSize=11.sp,color=Muted)
        Spacer(Modifier.height(26.dp))
    }
}

@Composable private fun LivePreview(config:SceneConfig,fps:Int,gyro:Boolean,modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val view=remember(context){NoxSurfaceView(context)}
    var err by remember {mutableStateOf<String?>(null)}
    view.onError = { message: String -> err=message }
    LifecycleResumeEffect(view,gyro) {
        view.resumeRender()
        val sensor=MotionSensor(context){x,y->view.tilt(x,y)}
        if(gyro) sensor.start() else view.tilt(0f,0f)
        onPauseOrDispose {sensor.stop();view.pauseRender()}
    }
    Box(modifier.background(Ink)) {
        AndroidView(factory={view},update={it.configure(config,fps)},modifier=Modifier.fillMaxSize())
        if(err!=null) Text("OpenGL: $err",color=Color.Red,modifier=Modifier.align(Alignment.Center).padding(12.dp))
    }
}
