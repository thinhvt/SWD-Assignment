package assignment.mangaholic.presenter.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import assignment.mangaholic.R;
import assignment.mangaholic.model.dbhelper.DatabaseHelper;
import assignment.mangaholic.model.dbhelper.MangaTable;
import assignment.mangaholic.presenter.eventhandlers.ChapterList;
import assignment.mangaholic.presenter.eventhandlers.MangaList;
import assignment.mangaholic.util.AllMangaList;
import assignment.mangaholic.view.MangaDetail;
import assignment.mangaholic.presenter.eventhandlers.ImageList;
import core.Chapter;
import core.Manga;
import core.truyentranh8.TruyenTranh8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GetMangaDataTask extends AsyncTask<String, Void, Manga> {
    TruyenTranh8 tt8;
    private Context context;
    private ImageList imageList;

    public GetMangaDataTask(Context c) {
        context = c;
        tt8 = new TruyenTranh8();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MangaDetail.hideAllView();
    }

    @Override
    protected Manga doInBackground(String... strings) {
        Manga manga = null;

        try{
            manga = tt8.getMangaInfo(strings[0]);
        } catch (NullPointerException e) {
            Log.e("EXCEPTION", e.getMessage());
        }
        return manga;
    }

    @Override
    protected void onPostExecute(final Manga manga) {
        super.onPostExecute(manga);

        TextView txtName = MangaDetail.txtName;

        if (manga == null) {
            txtName.setText("Manga đã bị xóa vì lú do bản quyền");
            return;
        }

        // prepare view
        final ListView chapterList = MangaDetail.chapterList;
        TextView txtRating = MangaDetail.txtRating;
        TextView txtAuthor = MangaDetail.txtAuthor;
        TextView txtTag = MangaDetail.txtTag;
        TextView txtStatus = MangaDetail.txtStatus;
        android.support.v7.widget.Toolbar toolbar = MangaDetail.toolbar;
        EditText txtSearchChapter = MangaDetail.txtSearchChapter;
        final ImageButton btnFavorite = MangaDetail.btnFavorite;

        // prepare strings
        StringBuilder authos = new StringBuilder("");
        StringBuilder tags = new StringBuilder("");
        for(String author : manga.getAuthors()) {
            authos.append(author + ", ");
        }

        // remove last ", "
        int uselessCommandIndex = authos.lastIndexOf(", ");
        if (uselessCommandIndex != -1)
            authos.replace(uselessCommandIndex , authos.length(), "");
        for(String tag : manga.getTags()) {
            tags.append(tag + ", ");
        }
        tags.replace(tags.lastIndexOf(", "), tags.length(), "");

        // set info
        txtName.setText("Truyện: " + manga.getName());
        txtRating.setText("Đánh giá: " + manga.getRating() + "");
        txtAuthor.setText("Tác giả: " + authos);
        txtTag.setText("Thể loại: " + tags);
        txtStatus.setText("Trạng thái: " + manga.getStatus());
        toolbar.setTitle(manga.getName());

        Manga curManga = MangaTable.findCurManga(DatabaseHelper.getHelper(context), manga.getUrl());
        if(curManga != null) {
            if (curManga.isFavourite())
                btnFavorite.setBackgroundResource(R.drawable.ic_favorite_white_24dp);
            else
                btnFavorite.setBackgroundResource(R.drawable.ic_favorite_border_white_24dp);
        }
        // done set info
        MangaDetail.showMangaInfo();

        List<Chapter> sortedChapterList = manga.getChapters();
        Collections.reverse(sortedChapterList);
        // show chapter list
        final ChapterList cl = new ChapterList(context, (ArrayList) sortedChapterList);
        chapterList.setOnItemClickListener(cl);
        chapterList.setAdapter(cl);
        MangaDetail.showChapterList();

        // download thumbnail
        DownloadMangaThumbnailTask downloadThumbnailTask = new DownloadMangaThumbnailTask();
        downloadThumbnailTask.execute(manga.getMangaThumbnail());


        //Event handlers for search
        txtSearchChapter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                cl.getFilter().filter(editable.toString());
            }
        });

        //add/remove favorite manga
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manga.setFavourite(!manga.isFavourite());
                MangaTable.updateFavorite(DatabaseHelper.getHelper(context), manga);

                new LoadAllMangaTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                if(manga.isFavourite())
                    btnFavorite.setBackgroundResource(R.drawable.ic_favorite_white_24dp);
                else
                    btnFavorite.setBackgroundResource(R.drawable.ic_favorite_border_white_24dp);
            }
        });
    }
}
