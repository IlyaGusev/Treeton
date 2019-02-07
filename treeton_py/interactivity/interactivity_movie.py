import yaml
import os

class PlaylistInfo(object):
    def __init__(self, playlist_path, movie_id, node_id):
        self.chunks = []
        self.upper_duration_bound = None
        self.hls_version = None

        with open(playlist_path, "r") as playlist:
            for line in playlist:
                line = line.strip()
                if line.startswith('#EXTINF'):
                    self.chunks.append([float(line.rstrip(',').split(':')[1]), None,])
                elif not line.startswith('#') and self.chunks[-1][1] is None:
                    self.chunks[-1][1] = '%s/%s/%s' % (movie_id, node_id , line)
                elif line.startswith('#EXT-X-TARGETDURATION'):
                    self.upper_duration_bound = int(line.split(':')[1])
                elif line.startswith('#EXT-X-VERSION'):
                    self.hls_version = int(line.split(':')[1])

    def count_duration(self):
        duration = 0
        for chunk_duration, _ in self.chunks:
            duration += chunk_duration
        return duration

def generate_playlist_string(version, target_duration, media_sequence, is_vod, chunks, chunk_prefix, gen_endlist):
    strings = [
        '#EXTM3U',
        '#EXT-X-PLAYLIST-TYPE:%s' % ('VOD' if is_vod else 'EVENT'),
        '#EXT-X-START:TIME-OFFSET=0',
        '#EXT-X-TARGETDURATION:%d' % target_duration,
        '#EXT-X-VERSION:%d' % version,
        '#EXT-X-MEDIA-SEQUENCE:%d' % media_sequence,
    ]

    for chunk in chunks:
        if chunk:
            strings.append('#EXTINF:%f,' % chunk[0])
            strings.append(chunk_prefix + chunk[1])
        else:
            strings.append('#EXT-X-DISCONTINUITY')

    if gen_endlist:
        strings.append('#EXT-X-ENDLIST')
    strings.append('')

    return '\n'.join(strings)

class InteractiveMovieFragment(object):
    def _load_video_info(self, movie_dir, node_id):
        self.video_info = {}
        self.duration = None
        self.hls_version = None
        self.upper_duration_bound = None
        self.number_of_chunks = None

        node_dir = os.path.join(movie_dir, node_id)
        movie_id = os.path.basename(movie_dir)

        for quality_marker in ['360','480','720']:
            playlist_path = os.path.join(node_dir, quality_marker + 'p.m3u8')
            if os.path.exists(playlist_path):
                playlist_info = PlaylistInfo(playlist_path, movie_id, node_id)
                self.video_info[quality_marker] = playlist_info
                d = playlist_info.count_duration()
                if self.duration is None:
                    self.duration = d
                else:
                    assert self.duration == d

                if self.upper_duration_bound is None:
                    self.upper_duration_bound = playlist_info.upper_duration_bound
                else:
                    assert self.upper_duration_bound == playlist_info.upper_duration_bound

                if self.hls_version is None:
                    self.hls_version = playlist_info.hls_version
                else:
                    assert self.hls_version == playlist_info.hls_version

                if self.number_of_chunks is None:
                    self.number_of_chunks = len(playlist_info.chunks)
                else:
                    assert self.number_of_chunks == len(playlist_info.chunks)

    def __init__(self, node_id, node_info, movie_dir):
        self.id = node_id
        if node_info and 'bifurcation' in node_info:
            bifurcation_info = node_info['bifurcation']
            self.bifurcation_timing = bifurcation_info['timing']
            self.bifurcation_length = bifurcation_info['length']
            self.bifurcation_options = {}
            for option_name, option_info in bifurcation_info['options'].items():
                self.bifurcation_options[option_name] = [
                    option_info['title'], option_info.get('discontinuity', True), option_info['target_id']
                ]
            self._bifurcation_default = bifurcation_info['default']
        else:
            self.bifurcation_timing = None
            self.bifurcation_length = None
            self.bifurcation_options = None
            self.bifurcation_default = None

        self._load_video_info(movie_dir, node_id)

class InteractiveMovie(object):
    def __init__(self, config_path):
        self.fragments = {}

        config = yaml.load(open(config_path))
        movie_dir = os.path.dirname(config_path)

        self.id = os.path.basename(movie_dir)

        for node_id, node_info in config.items():
            if node_id == 'root':
                assert isinstance(node_info, str)
                self.root = node_info
            else:
                self.fragments[node_id] = InteractiveMovieFragment(node_id, node_info, movie_dir)

        self.root = self.fragments[self.root]

        for _, fragment in self.fragments.items():
            if fragment.bifurcation_options:
                for opt_name, opt_info in fragment.bifurcation_options.items():
                    opt_info[2] = self.fragments[opt_info[2]]
