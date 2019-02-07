import yaml
import argparse
import os
import subprocess

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Prepare interactive movie sources for hls streaming')
    parser.add_argument('config_path', metavar='SOURCE_MOVIE_CONFIG_PATH', help='path to yaml-config (all relative'
                                                                         ' paths within the config file are counted '
                                                                         'from the directory to which it belongs)')
    args = parser.parse_args()
    config = yaml.load(open(args.config_path))
    movie_dir = os.path.dirname(args.config_path)
    node_dir_info = {}

    for node_name, node in config.items():
        if node_name == 'root':
            continue
        node_dir = os.path.join(movie_dir, node_name)
        if os.path.isdir(node_dir):
            print('Found directory for node %s' % node_name)
        else:
            raise RuntimeError('Unable to find directory for node %s' % node_name)

        source_path = os.path.join(node_dir, node_name + '.mp4')
        if not os.path.exists(source_path) or not os.path.isfile(source_path):
            source_path = os.path.join(node_dir, 'source.mp4')
            if not os.path.exists(source_path) or not os.path.isfile(source_path):
                raise RuntimeError('Unable to find source mp4 for node %s' % node_name)

        print('Found source mp4 for node %s: %s' % (node_name, source_path))
        node_dir_info[node_dir] = os.path.basename(source_path)

    wd = os.getcwd()
    for node_dir, source_name in node_dir_info.items():
        os.chdir(node_dir)
        subprocess.run([
            "ffmpeg","-hide_banner","-y","-i",source_name,
            "-vf","scale=w=640:h=360:force_original_aspect_ratio=decrease","-c:a","aac","-ar","48000","-c:v","h264","-profile:v","main","-sc_threshold","0","-copyts","-copytb","0","-hls_time","4","-hls_playlist_type","event","-b:v","800k","-maxrate","856k","-bufsize","1200k","-b:a","96k","-hls_segment_filename","360p_%03d.ts","360p.m3u8",
            "-vf","scale=w=842:h=480:force_original_aspect_ratio=decrease","-c:a","aac","-ar","48000","-c:v","h264","-profile:v","main","-sc_threshold","0","-copyts","-copytb","0","-hls_time","4","-hls_playlist_type","event","-b:v","1400k","-maxrate","1498k","-bufsize","2100k","-b:a","128k","-hls_segment_filename","480p_%03d.ts","480p.m3u8",
            "-vf","scale=w=1280:h=720:force_original_aspect_ratio=decrease","-c:a","aac","-ar","48000","-c:v","h264","-profile:v","main","-sc_threshold","0","-copyts","-copytb","0","-hls_time","4","-hls_playlist_type","event","-b:v","2800k","-maxrate","2996k","-bufsize","4200k","-b:a","128k","-hls_segment_filename","720p_%03d.ts","720p.m3u8"
        ])
        os.chdir(wd)
