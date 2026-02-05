$lines = Get-Content "C:\Users\27350\Desktop\mpvEx-master\app\src\main\res\values\strings.xml"
$lines[241] = '    <string name="pref_anime4k_incompatibility">Anime4K 不兼容</string>'
$lines[242] = '    <string name="pref_anime4k_gpu_next_error">gpu-next 与 Anime4K 着色器不兼容。启用 Anime4K 时，应用程序将自动使用传统的 gpu 后端以保持着色器兼容性。</string>'
$lines[243] = '    <string name="pref_anime4k_cannot_use_with_gpu_next">无法在启用 gpu-next 时使用 Anime4K</string>'
Set-Content "C:\Users\27350\Desktop\mpvEx-master\app\src\main\res\values\strings.xml" $lines -Encoding UTF8
