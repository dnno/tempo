package com.cappielloantonio.play.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.cappielloantonio.play.App;
import com.cappielloantonio.play.R;
import com.cappielloantonio.play.adapter.SongHorizontalAdapter;
import com.cappielloantonio.play.databinding.FragmentAlbumPageBinding;
import com.cappielloantonio.play.glide.CustomGlideRequest;
import com.cappielloantonio.play.repository.QueueRepository;
import com.cappielloantonio.play.service.MusicPlayerRemote;
import com.cappielloantonio.play.ui.activity.MainActivity;
import com.cappielloantonio.play.util.DownloadUtil;
import com.cappielloantonio.play.util.MusicUtil;
import com.cappielloantonio.play.viewmodel.AlbumPageViewModel;

import java.util.Collections;
import java.util.Objects;

public class AlbumPageFragment extends Fragment {
    private static final String TAG = "AlbumPageFragment";

    private FragmentAlbumPageBinding bind;
    private MainActivity activity;
    private AlbumPageViewModel albumPageViewModel;

    private SongHorizontalAdapter songHorizontalAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.album_page_menu, menu);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        bind = FragmentAlbumPageBinding.inflate(inflater, container, false);
        View view = bind.getRoot();
        albumPageViewModel = new ViewModelProvider(requireActivity()).get(AlbumPageViewModel.class);

        init();
        initAppBar();
        initAlbumInfoTextButton();
        initMusicButton();
        initBackCover();
        initSongsView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.setBottomNavigationBarVisibility(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_download_album) {
            albumPageViewModel.getAlbumSongLiveList(requireActivity()).observe(requireActivity(), songs -> {
                if (isVisible() && getActivity() != null) {
                    DownloadUtil.getDownloadTracker(requireContext()).download(songs, null, null);
                }
            });
            return true;
        }

        return false;
    }

    private void init() {
        albumPageViewModel.setAlbum(requireArguments().getParcelable("album_object"));
        albumPageViewModel.setOffline(requireArguments().getBoolean("is_offline"));
    }

    private void initAppBar() {
        activity.setSupportActionBar(bind.animToolbar);

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        bind.animToolbar.setTitle(MusicUtil.getReadableString(albumPageViewModel.getAlbum().getTitle()));

        bind.albumNameLabel.setText(MusicUtil.getReadableString(albumPageViewModel.getAlbum().getTitle()));
        bind.albumArtistLabel.setText(MusicUtil.getReadableString(albumPageViewModel.getAlbum().getArtistName()));
        bind.albumReleaseYearLabel.setText(albumPageViewModel.getAlbum().getYear() != 0 ? String.valueOf(albumPageViewModel.getAlbum().getYear()) : "");

        bind.animToolbar.setNavigationOnClickListener(v -> activity.navController.navigateUp());

        Objects.requireNonNull(bind.animToolbar.getOverflowIcon()).setTint(requireContext().getResources().getColor(R.color.titleTextColor, null));
    }

    private void initAlbumInfoTextButton() {
        bind.albumArtistLabel.setOnClickListener(v -> albumPageViewModel.getArtist().observe(requireActivity(), artist -> {
            if (artist != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("artist_object", artist);
                activity.navController.navigate(R.id.action_albumPageFragment_to_artistPageFragment, bundle);
            } else
                Toast.makeText(requireContext(), "Error retrieving artist", Toast.LENGTH_SHORT).show();
        }));
    }

    private void initMusicButton() {
        albumPageViewModel.getAlbumSongLiveList(requireActivity()).observe(requireActivity(), songs -> {
            if (bind != null && !songs.isEmpty()) {
                bind.albumPagePlayButton.setOnClickListener(v -> {
                    QueueRepository queueRepository = new QueueRepository(App.getInstance());
                    queueRepository.insertAllAndStartNew(songs);

                    activity.isBottomSheetInPeek(true);
                    activity.setBottomSheetMusicInfo(songs.get(0));

                    MusicPlayerRemote.openQueue(songs, 0, true);
                });

                bind.albumPageShuffleButton.setOnClickListener(v -> {
                    Collections.shuffle(songs);

                    QueueRepository queueRepository = new QueueRepository(App.getInstance());
                    queueRepository.insertAllAndStartNew(songs);

                    activity.isBottomSheetInPeek(true);
                    activity.setBottomSheetMusicInfo(songs.get(0));

                    MusicPlayerRemote.openQueue(songs, 0, true);
                });
            }

            if (bind != null && songs.isEmpty()) {
                bind.albumPagePlayButton.setEnabled(false);
                bind.albumPageShuffleButton.setEnabled(false);
            }
        });
    }

    private void initBackCover() {
        CustomGlideRequest.Builder
                .from(requireContext(), albumPageViewModel.getAlbum().getPrimary(), CustomGlideRequest.ALBUM_PIC, null)
                .build()
                .transform(new RoundedCorners(CustomGlideRequest.CORNER_RADIUS))
                .into(bind.albumCoverImageView);
    }

    private void initSongsView() {
        bind.songRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.songRecyclerView.setHasFixedSize(true);

        songHorizontalAdapter = new SongHorizontalAdapter(activity, requireContext(), false);
        bind.songRecyclerView.setAdapter(songHorizontalAdapter);

        albumPageViewModel.getAlbumSongLiveList(requireActivity()).observe(requireActivity(), songs -> songHorizontalAdapter.setItems(songs));
    }
}