package com.softensy.customcamera.fileUtil;


import android.os.Environment;

public class MergeVideoAsync {

//    public class MergeVideo extends AsyncTask<String, Integer, String> {
//
//        int count;
//        String filename;
//
//
//        @Override
//        protected void onPreExecute() {
//
//        };
//
//        @Override
//        protected String doInBackground(String... params) {
//            try {
//                String paths[] = new String[count];
//                Movie[] inMovies = new Movie[count];
//                for (int i = 0; i < count; i++) {
//                    paths[i] = path + filename + String.valueOf(i + 1) + ".mp4";
//                    inMovies[i] = MovieCreator.build(new FileInputStream(paths[i]).getChannel());
//                }
//                List<Track> videoTracks = new LinkedList<Track>();
//                List<Track> audioTracks = new LinkedList<Track>();
//                for (Movie m : inMovies) {
//                    for (Track t : m.getTracks()) {
//                        if (t.getHandler().equals("soun")) {
//                            audioTracks.add(t);
//                        }
//                        if (t.getHandler().equals("vide")) {
//                            videoTracks.add(t);
//                        }
//                    }
//                }
//
//                Movie result = new Movie();
//
//                if (audioTracks.size() > 0) {
//                    result.addTrack(new AppendTrack(audioTracks
//                            .toArray(new Track[audioTracks.size()])));
//                }
//                if (videoTracks.size() > 0) {
//                    result.addTrack(new AppendTrack(videoTracks
//                            .toArray(new Track[videoTracks.size()])));
//                }
//
//                BasicContainer out = (BasicContainer) new DefaultMp4Builder()
//                        .build(result);
//
//                @SuppressWarnings("resource")
//                FileChannel fc = new RandomAccessFile(String.format(Environment
//                        .getExternalStorageDirectory() + "/wishbyvideo.mp4"),
//                        "rw").getChannel();
//                out.writeContainer(fc);
//                fc.close();
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//            String mFileName = Environment.getExternalStorageDirectory()
//                    .getAbsolutePath();
//            mFileName += "/wishbyvideo.mp4";
//            filename = mFileName;
//            return mFileName;
//        }
//
//        @Override
//        protected void onPostExecute(String value) {
//            super.onPostExecute(value);
//
//
//
//        }
//    }
}
