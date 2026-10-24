"""Offline consistency checks only; does NOT assert Android build or device correctness."""
import pathlib
import re
import unittest
import xml.etree.ElementTree as ET

ROOT=pathlib.Path(__file__).resolve().parent.parent

class StructuralContracts(unittest.TestCase):
    def read(self,path):return (ROOT/path).read_text(encoding='utf-8')
    def test_no_internet_or_storage_permissions(self):
        for path in ROOT.glob('**/AndroidManifest.xml'):
            text=path.read_text(encoding='utf-8')
            self.assertNotIn('<uses-permission',text)
            ET.fromstring(text)
    def test_module_layout(self):
        root=self.read('settings.gradle.kts')
        for name in ['app','core:model','core:renderer','core:data','feature:wallpaper']:
            self.assertIn(f'":{name}"',root)
    def test_live_wallpaper_is_exported_and_guarded(self):
        s=self.read('feature/wallpaper/src/main/AndroidManifest.xml')
        self.assertIn('android.permission.BIND_WALLPAPER',s)
        self.assertIn('android.service.wallpaper.WallpaperService',s)
        source=self.read('feature/wallpaper/src/main/java/studio/nox/wallpaper/NoxWallpaperService.kt')
        for case in ['onSurfaceCreated','onSurfaceDestroyed','onVisibilityChanged','motion.stop()','setVisible(isVisible)']:
            self.assertIn(case,source)
    def test_living_scenes_kotlin_imports_remain_consistent(self):
        source=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        for symbol in ('SceneLooks','SceneLook','ScenePresets','SceneRegistry',
                       'SceneCategory','NamedScenePreset'):
            self.assertEqual(source.count('import studio.nox.model.'+symbol+'\n'),1,
                             symbol+' import must be present exactly once')
        self.assertNotIn('import studio.nox.model.NamedScenePresets\n',source)

    def test_living_scenes_browse_six_with_scene_variants(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        home=ui.split('@Composable private fun HomeScreen(',1)[1].split('@Composable private fun StudioScreen(',1)[0]
        self.assertIn('horizontalScroll(rememberScrollState())',home)
        self.assertIn('SceneRegistry.get(id)?.isNew',home)
        self.assertIn('ScenePresets.forScene(scene)',home)
        self.assertIn('onVariant(variant)',home)
        self.assertIn('homeDraft=variant.config',ui)
        self.assertIn('onExport(config)',home)
        self.assertIn('LivePreview(config,fps,gyro,quality,Modifier.fillMaxSize())',home)

    def test_new_scenes_use_canvas_minis_and_category_filter(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        thumb=ui.split('@Composable private fun Miniature(',1)[1].split('@Composable private fun SettingsScreen(',1)[0]
        gallery=ui.split('@Composable private fun SearchableLibrary(',1)[1].split('@Composable private fun Miniature(',1)[0]
        for scene in ('"AURORA" ->', '"PULSE" ->', '"COMET" ->'):
            self.assertIn(scene,thumb)
        self.assertIn('SceneRegistry.get(preset.scene.sceneId)?.category',gallery)
        self.assertIn('filterCategory',gallery)
        self.assertIn('SceneCategory.AMBIENT',gallery)
        self.assertIn('filterFavorites',gallery)
        self.assertIn('GridItemSpan(2)',gallery)

    def test_graphics_overdrive_quality_is_persistent_and_live(self):
        data=self.read('core/data/src/main/java/studio/nox/data/SettingsRepository.kt')
        viewmodel=self.read('app/src/main/java/studio/nox/app/NoxViewModel.kt')
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        surface=self.read('core/renderer/src/main/java/studio/nox/renderer/NoxSurfaceView.kt')
        egl=self.read('core/renderer/src/main/java/studio/nox/renderer/EglSession.kt')
        wallpaper=self.read('feature/wallpaper/src/main/java/studio/nox/wallpaper/NoxWallpaperService.kt')
        gl=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        for term in ('intPreferencesKey("visual_quality")','val quality:Int=1','suspend fun setQuality'):
            self.assertIn(term,data)
        self.assertIn('fun setQuality(value:Int)',viewmodel)
        for term in ('settings.quality','onQuality=vm::setQuality','Rich','Ultra','Balanced'):
            self.assertIn(term,ui)
        self.assertIn('it.configure(config,fps,quality)',ui)
        self.assertIn('FullscreenPreview(config,fps,gyro,quality',ui)
        self.assertIn('private fun FullscreenPreview(config:SceneConfig,fps:Int,gyro:Boolean,quality:Int',ui)
        self.assertIn('fun configure(c:SceneConfig,frames:Int,quality:Int)',surface)
        self.assertIn('session?.setQuality(quality)',surface)
        self.assertIn('fun setQuality(value:Int)',egl)
        self.assertIn('VisualQuality.effective(',egl)
        self.assertIn('s.setQuality(last.quality)',wallpaper)
        self.assertIn('session?.setQuality(prefs.quality)',wallpaper)
        self.assertIn('uQuality',gl)

    def test_cinematic_depth_all_nine_are_quality_gated(self):
        src=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        self.assertIn('float smoothNoise(vec2 location)',src)
        self.assertIn('float cloudField(vec2 location)',src)
        for scene in ('FLUX','HALO','NEBULA','AURORA','PULSE','COMET','VEIL','GRID','SHARDS'):
            self.assertIn('// CINEMATIC '+scene+':',src)
        self.assertIn('vec3 cinematicDetail=vec3(0.0);',src)
        self.assertIn('sum+=cinematicDetail*qualityMix;',src)
        self.assertIn('if(uQuality>1.5)',src)
        self.assertIn('float microSpark=',src)
        self.assertIn('float activeLight=',src)
        self.assertIn('min(sum,vec3(1.0))',src)

    def test_volumetric_light_keeps_nine_shaped_depth_layers_quality_gated(self):
        shader=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        volumetric=shader.split('vec3 cinematicDetail=vec3(0.0);',1)[1].split('sum+=cinematicDetail*qualityMix;',1)[0]
        self.assertIn('vec3 shapedVolume=vec3(0.0);',volumetric)
        for scene in ('FLUX','HALO','NEBULA','AURORA','PULSE','COMET','VEIL','GRID','SHARDS'):
            self.assertIn('// VOLUMETRIC '+scene+':',volumetric,scene+' must have a distinct lit 3D mass')
        self.assertGreaterEqual(volumetric.count('shapedVolume+='),9,
            'Each scene should shape its own illuminated material, not share a blanket glow')
        self.assertIn('shapedVolume*=smoothstep(',volumetric,
            'Suppress low-level haze in deep black pixels')
        self.assertIn('sum+=shapedVolume*qualityMix;',shader,
            'Only Rich and Ultra may add 1.9.2 volume')
        self.assertIn('if(uQuality>0.5)',shader,
            'Balanced must retain its lean pipeline')

    def test_graphics_overdrive_has_real_depth_for_all_nine_modes(self):
        gl=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        for term in ('// OVERDRIVE FLUX','// OVERDRIVE HALO','// OVERDRIVE NEBULA',
                     '// OVERDRIVE AURORA','// OVERDRIVE PULSE','// OVERDRIVE COMET',
                     '// OVERDRIVE VEIL','// OVERDRIVE GRID','// OVERDRIVE SHARDS'):
            self.assertIn(term,gl)
        self.assertIn('float qualityMix=',gl)
        self.assertIn('vec3 premiumDetail=',gl)
        self.assertIn('uQuality',gl)
        self.assertIn('min(sum,vec3(1.0))',gl)

    def test_spectra_modes_keep_previous_six_branches(self):
        shader=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        for name,mode in (('VEIL','6f'),('GRID','7f'),('SHARDS','8f')):
            self.assertIn('"'+name+'"->'+mode,shader)
        for label in ('// VEIL 1.8:','// GRID 1.8:','// SHARDS 1.8:'):
            self.assertIn(label,shader)
        for bound in ('else if (uMode < 5.5)','else if (uMode < 6.5)',
                      'else if (uMode < 7.5)'):
            self.assertIn(bound,shader)
        self.assertIn('GL_TRIANGLE_STRIP',shader)
        self.assertIn('uniform vec2 uResolution, uMotion, uTouch;',shader)
        self.assertIn('// COMET 1.7.1: depth-layered trails',shader)
        self.assertNotIn('float active=',shader)

    def test_spectra_gallery_canvas_and_home(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        home=ui.split('@Composable private fun HomeScreen(',1)[1].split('@Composable private fun StudioScreen(',1)[0]
        miniature=ui.split('@Composable private fun Miniature(',1)[1].split('@Composable private fun SettingsScreen(',1)[0]
        for fragment in ('"VEIL" ->','"GRID" ->','"SHARDS" ->'):
            self.assertIn(fragment,miniature)
        for label in ('"VEIL" -> "','"GRID" -> "','"SHARDS" -> "'):
            self.assertIn(label,home)
        self.assertIn('SceneCatalog.ids.forEach',home)
        self.assertIn('onSurprise',home)
        self.assertIn('SceneSurprise.choose(homeScene',ui)
        self.assertIn('homeDraft=next',ui)
        self.assertIn('ScenePresets.forScene(scene)',home)
        self.assertIn('LivePreview(config,fps,gyro,quality,Modifier.fillMaxSize())',home)

    def test_light_dynamics_three_live_scenes(self):
        src=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        for expected in (
            '// AURORA 1.7.1: layered curtains',
            'float curtainDepth',
            'float innerFilament',
            '// PULSE 1.7.1: traveling waves',
            'float life=fract(',
            'float cycleFade=',
            'vec2 focus=',
            '// COMET 1.7.1: depth-layered trails',
            'float depth=',
            'float visibilityMask=step(',
            'float trailFade='
        ):
            self.assertIn(expected,src)
        # Preserve all existing modes and legacy scene shaders.
        for mode in ('"FLUX"->0f','"HALO"->1f','"NEBULA"->2f',
                     '"AURORA"->3f','"PULSE"->4f','"COMET"->5f'):
            self.assertIn(mode,src)
        self.assertIn('// FLUX: staggered luminous ribbons',src)
        self.assertIn('// HALO: depth-stacked elliptical orbits',src)
        self.assertIn('// NEBULA: stable star positions',src)

    def test_living_scenes_gl_modes_and_legacy_branch(self):
        shader=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        for mode in ('"FLUX"->0f','"HALO"->1f','"NEBULA"->2f',
                     '"AURORA"->3f','"PULSE"->4f','"COMET"->5f'):
            self.assertIn(mode,shader)
        for branch in ('if(uMode < 0.5)','else if (uMode < 1.5)',
                       'else if (uMode < 2.5)','else if (uMode < 3.5)',
                       'else if (uMode < 4.5)','COMET 1.7.1:'):
            self.assertIn(branch,shader)
        self.assertIn('GL_TRIANGLE_STRIP',shader)
        self.assertIn('uMotion',shader)
        self.assertIn('uTouch',shader)

    def test_offline_gl_materials(self):
        shaders=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        for case in ['FLUX:','HALO:','NEBULA:','GL_TRIANGLE_STRIP','gl_FragColor']:
            self.assertIn(case,shaders)
    def test_document_export_and_presets(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        self.assertIn('CreateDocument("image/png")',ui)
        self.assertIn('compress(Bitmap.CompressFormat.PNG',ui)
        repo=self.read('core/data/src/main/java/studio/nox/data/PresetEntity.kt')
        for case in ['@Database','@Dao','fun save(','fun duplicate(','fun delete(']:self.assertIn(case,repo)
    def test_compose_bom_compatible_with_compile_sdk36(self):
        app=self.read('app/build.gradle.kts')
        self.assertIn('compileSdk = 36', app)
        self.assertIn('compose-bom:2025.10.01', app,
                          'Compile SDK 36 / AGP 8.13 cannot build Compose BOM 2026.09 (Compose 1.12 needs SDK 37 / AGP 9.1)')
    def test_room_supertype_visible_to_consuming_android_modules(self):
        data=self.read('core/data/build.gradle.kts')
        self.assertIn('api("androidx.room:room-runtime:2.8.5")', data,
            'NoxDatabase publicly inherits RoomDatabase; its dependency must be api, not implementation')
    def test_studio_preview_remains_visible_while_controls_scroll(self):
        source = self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        studio = source.split('@Composable private fun StudioScreen(', 1)[1].split('@Composable private fun FullscreenPreview(', 1)[0]
        self.assertIn('BoxWithConstraints(Modifier.fillMaxSize())', studio)
        self.assertIn('Column(Modifier.fillMaxSize().padding(horizontal=18.dp))', studio)
        pinned_preview = studio.index('LivePreview(displayConfig,fps,gyro,quality,Modifier.fillMaxSize())')
        scrollable_controls = studio.index('Column(Modifier.weight(1f).verticalScroll(rememberScrollState()))')
        self.assertLess(pinned_preview, scrollable_controls)
        self.assertEqual(studio.count('verticalScroll(rememberScrollState())'), 1,
                         'Only editing controls must scroll, not the preview or header')
        self.assertIn('previewHeight', studio)
        self.assertIn('FullscreenPreview(config,fps,gyro', studio)

    def test_atelier_cinematic_home_with_working_remix(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        home=ui.split('@Composable private fun HomeScreen(',1)[1].split('@Composable private fun StudioScreen(',1)[0]
        app=ui.split('@Composable fun NoxApp(',1)[1].split('@Composable private fun HomeScreen(',1)[0]
        self.assertIn('homeDraft',app)
        self.assertIn('SceneLooks.shuffle(',app)
        self.assertIn('onRemix',home)
        self.assertIn('Modifier.weight(1f)',home)
        self.assertIn('onExport(config)',home)
        self.assertIn('SceneMiniature(',home)
        self.assertIn('saveAndApply',app)
        self.assertIn('onApply=',app)

    def test_atelier_studio_actions_fixed_below_controls(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        studio=ui.split('@Composable private fun StudioScreen(',1)[1].split('@Composable private fun FullscreenPreview(',1)[0]
        self.assertIn('Column(Modifier.weight(1f).verticalScroll(rememberScrollState()))',studio)
        self.assertIn('SaveActionDock(',studio)
        self.assertLess(studio.index('Column(Modifier.weight(1f).verticalScroll(rememberScrollState()))'),studio.index('SaveActionDock('))
        self.assertIn('LivePreview(displayConfig',studio)

    def test_collection_has_full_width_last_card_and_refined_menu(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        section=ui.split('@Composable private fun SearchableLibrary(',1)[1].split('@Composable private fun Miniature(',1)[0]
        self.assertIn('GridItemSpan(2)',section)
        self.assertIn('filtered.size % 2 == 1',section)
        self.assertIn('items(count=filtered.size',section)
        self.assertIn('onEdit(preset)',section)
        self.assertIn('onUse(preset)',section)
        self.assertIn('onDuplicate(preset)',section)
        self.assertIn('confirming=preset',section)
        self.assertIn('active',section)

    def test_settings_three_sections_and_real_controls(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        section=ui.split('@Composable private fun SettingsScreen(',1)[1].split('@Composable private fun LivePreview(',1)[0]
        for label in ('Производительность','Интерактивность','Конфиденциальность и движок',
                      'Экономия','Баланс','Максимум','Без аккаунта','Без интернета','Локальное хранение'):
            self.assertIn(label,section)
        self.assertIn('onFps(cap)',section)
        self.assertIn('Switch(checked=gyro,onCheckedChange=onGyro',section)
        self.assertIn('if(gyro) "Включено" else "Выключено"',section)

    def test_curated_favorites_are_persistent_and_wired(self):
        data=self.read('core/data/src/main/java/studio/nox/data/SettingsRepository.kt')
        vm=self.read('app/src/main/java/studio/nox/app/NoxViewModel.kt')
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        gallery=ui.split('@Composable private fun SearchableLibrary(',1)[1].split('@Composable private fun Miniature(',1)[0]
        self.assertIn('stringSetPreferencesKey("favorite_ids")',data)
        self.assertIn('suspend fun setFavorite(',data)
        self.assertIn('favoriteIds',data)
        self.assertIn('fun toggleFavorite(',vm)
        self.assertIn('filterFavorites',gallery)
        self.assertIn('Избранное',gallery)
        self.assertIn('onToggleFavorite(preset)',gallery)
        self.assertIn('Icons.Outlined.Star',gallery)
        self.assertIn('favoriteIds=settings.favoriteIds',ui)

    def test_curated_home_quick_looks_use_scene_draft(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        home=ui.split('@Composable private fun HomeScreen(',1)[1].split('@Composable private fun SceneMiniature(',1)[0]
        self.assertIn('onLook',home)
        self.assertIn('ModalBottomSheet(',home)
        self.assertIn('SceneLooks.looks.forEach',home)
        self.assertIn('onLook(look)',home)
        self.assertIn('homeDraft=SceneLooks.apply(previewHomeConfig,look)',ui)
        self.assertIn('onExport(config)',home)
        self.assertIn('onRemix',home)

    def test_portable_preset_actions_and_gallery_sort(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        vm=self.read('app/src/main/java/studio/nox/app/NoxViewModel.kt')
        gallery=ui.split('@Composable private fun SearchableLibrary(',1)[1].split('@Composable private fun Miniature(',1)[0]
        settings=ui.split('@Composable private fun SettingsScreen(',1)[1].split('@Composable private fun LivePreview(',1)[0]
        for fragment in ('PresetTransfer.encode(', 'PresetTransfer.decode(',
                         'CreateDocument("text/plain")', 'OpenDocument()','importPortable('):
            self.assertIn(fragment,ui+vm)
        self.assertIn('sortOrder',gallery)
        self.assertIn('sortedBy',gallery)
        self.assertIn('onExportPresets',settings)
        self.assertIn('onImportPresets',settings)
        self.assertIn('NOX ${BuildConfig.VERSION_NAME} Curated',ui)

    def test_version_header_tracks_build_config(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        app=self.read('app/build.gradle.kts')
        self.assertIn('BuildConfig.VERSION_NAME',ui)
        self.assertIn('buildConfig = true',app)

    def test_nebula_uses_trig_free_hash(self):
        src=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        self.assertIn('float hash(vec2 p)',src)
        h=src.split('float hash(vec2 p)',1)[1].split('float lightline',1)[0]
        self.assertNotIn('sin(',h,'Randomized stars must avoid sin*large multiplication unstable on mobile GPUs')

    def test_atelier_gallery_is_filterable_visual_grid(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        library=ui.split('@Composable private fun SearchableLibrary(',1)[1].split('@Composable private fun Miniature(',1)[0]
        for fragment in ('LazyVerticalGrid(', 'GridCells.Fixed(2)', 'filterScene',
                         'SceneCatalog.ids', 'DropdownMenu(', 'onDuplicate(preset)',
                         'onUse(preset)', 'confirming=preset'):
            self.assertIn(fragment,library,fragment)

    def test_signature_editor_undo_and_precision(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        section=ui.split('@Composable private fun StudioScreen(',1)[1].split('@Composable private fun FullscreenPreview(',1)[0]
        app=ui.split('@Composable fun NoxApp(',1)[1].split('@Composable private fun HomeScreen(',1)[0]
        param=ui.split('@Composable private fun Parameter(',1)[1].split('@Composable private fun LibraryScreen(',1)[0]
        for fragment in ('EditHistory.start(', 'history.record(', 'history.undo()', 'history.redo()'):
            self.assertIn(fragment,app)
        for fragment in ('canUndo', 'canRedo', 'onUndo', 'onRedo', 'onLiveCommit'):
            self.assertIn(fragment,section)
        self.assertIn('Slider(',param)
        self.assertIn('onValueChangeFinished=onValueCommit',param)
        self.assertIn('onStep(',param)
        self.assertIn('LivePreview(displayConfig',section)
        self.assertEqual(section.count('Column(Modifier.weight(1f).verticalScroll(rememberScrollState()))'),1)

    def test_palette_lab_and_pinned_preview_tools(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        studio=ui.split('@Composable private fun StudioScreen(',1)[1].split('@Composable private fun FullscreenPreview(',1)[0]
        for item in ('ColorHarmony.Mode.entries', 'ColorHarmony.apply(config, mode)',
                     'compareOriginal', 'compactPreview', 'Оригинал',
                     'previewHeight = if (compactPreview)', 'LivePreview(displayConfig'):
            self.assertIn(item,studio)
        self.assertIn('Column(Modifier.weight(1f).verticalScroll(rememberScrollState()))',studio)
        self.assertEqual(studio.count('verticalScroll(rememberScrollState())'),1)
        self.assertLess(studio.index('LivePreview(displayConfig'),studio.index('Column(Modifier.weight(1f).verticalScroll(rememberScrollState()))'))

    def test_premium_studio_11_tools(self):
        ui=self.read('app/src/main/java/studio/nox/app/NoxApp.kt')
        for symbol in ['SceneLooks.looks', 'SceneLooks.shuffle(', 'FullscreenPreview(',
                       'SceneCatalog.fromHex(', 'Найти композицию', 'SearchableLibrary']:
            self.assertIn(symbol, ui, 'NOX 1.1 must expose real editor or discovery functionality: ' + symbol)
    def test_vivid_light_strengthens_subjects_without_lifting_black(self):
        # Shader constants are checked numerically; actual GLES compilation is
        # independently exercised by verify_glsl.py in Android CI.
        shader=self.read('core/renderer/src/main/java/studio/nox/renderer/GlBackend.kt')
        self.assertIn('vec3 spectralGrade=',shader)
        self.assertIn('sum=mix(sum,spectralGrade,luminousMask);',shader)
        quality=shader.split('if(uQuality>0.5)',1)[1]
        self.assertIn('sum=mix(sum,spectralGrade,luminousMask);',quality)
        expression=re.search(r'vec3 spectralGrade=\(sum\*([0-9.]+)\)/\(vec3\(1\.0\)\+sum\*([0-9.]+)\);',shader)
        self.assertIsNotNone(expression,'Missing bounded per-channel spectral exposure')
        exposure,shoulder=map(float,expression.groups())
        self.assertGreater(exposure,2.3)
        self.assertGreater(shoulder,1.4)
        mapped=lambda v:(v*exposure)/(1.0+v*shoulder)
        self.assertEqual(mapped(0.0),0.0,'True-black regions must stay black')
        self.assertGreater(mapped(0.12),0.22,'Weak light structures need a visible boost')
        self.assertLess(mapped(0.8),1.0,'Bright details need a shoulder, not clipping')
        self.assertIn('luminousMask=smoothstep(0.022,0.11,luminousSignal)',shader)
        self.assertIn('spectralLuma=dot(spectralGrade',shader)
        self.assertIn('mix(vec3(spectralLuma),spectralGrade,1.23)',shader)

    def test_vivid_preview_has_separate_versioned_package(self):
        app=self.read('app/build.gradle.kts')
        for contract in ['applicationId = "studio.nox.light"', 'applicationIdSuffix = ".vivid"', 'versionCode = 16',
                         'versionName = "1.9.3"']:
            self.assertIn(contract, app)
        manifest=self.read('app/src/debug/AndroidManifest.xml')
        self.assertIn('NOX 1.9.3 Vivid',manifest)
        workflow=self.read('.github/workflows/android.yml')
        self.assertIn('NOX-1.9.3-vivid-APK',workflow)
    def test_build_workflow_creates_apk(self):
        workflow=self.read('.github/workflows/android.yml')
        self.assertIn('gradle :app:assembleDebug',workflow)
        self.assertIn('app/build/outputs/apk/debug/app-debug.apk',workflow)

if __name__=='__main__':unittest.main(verbosity=2)
