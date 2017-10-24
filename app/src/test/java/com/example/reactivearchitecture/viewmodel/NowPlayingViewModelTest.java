package com.example.reactivearchitecture.viewmodel;

import android.app.Application;

import com.example.reactivearchitecture.categories.UnitTest;
import com.example.reactivearchitecture.gateway.ServiceGateway;
import com.example.reactivearchitecture.interactor.NowPlayingInteractor;
import com.example.reactivearchitecture.model.AdapterCommandType;
import com.example.reactivearchitecture.model.MovieInfo;
import com.example.reactivearchitecture.model.MovieInfoImpl;
import com.example.reactivearchitecture.model.MovieViewInfo;
import com.example.reactivearchitecture.model.UiModel;
import com.example.reactivearchitecture.model.action.Action;
import com.example.reactivearchitecture.model.action.ScrollAction;
import com.example.reactivearchitecture.model.event.ScrollEvent;
import com.example.reactivearchitecture.model.result.RestoreResult;
import com.example.reactivearchitecture.model.result.Result;
import com.example.reactivearchitecture.model.result.ScrollResult;
import com.example.reactivearchitecture.rx.RxJavaTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@Category(UnitTest.class)
public class NowPlayingViewModelTest extends RxJavaTest {
    @Mock
    ServiceGateway mockServiceGateway;

    @Mock
    Application mockApplication;

    @Mock
    NowPlayingInteractor mockNowPlayingInteractor;

    @Mock
    TestTransformer mockTestTransformer;

    MovieInfo movieInfo = new MovieInfoImpl(
            "www.url.com",
            "Dan The Man",
            new Date(),
            9d);

    @Before
    public void setUp() {
        super.setUp();
        initMocks(this);

        //Mock Publish() return.
        when(mockNowPlayingInteractor.processAction(any(Observable.class))).thenAnswer(new Answer<Observable<Result>>() {
            @Override
            public Observable<Result> answer(InvocationOnMock invocation) throws Throwable {
                Observable<Action> actionObservable = (Observable<Action>) invocation.getArguments()[0];

                return actionObservable.flatMap(new Function<Action, ObservableSource<Result>>() {
                    @Override
                    public ObservableSource<Result> apply(@NonNull Action action) throws Exception {
                        return mockTestTransformer.transform(action);
                    }
                });
            }
        });
    }

    @Test
    public void initState() {
        //
        //Arrange
        //
        TestObserver<UiModel> testObserver;
        TestNowPlayingViewModel nowPlayingViewModel = new TestNowPlayingViewModel(mockApplication,
                mockServiceGateway, mockNowPlayingInteractor);
        nowPlayingViewModel.init(null);
        when(mockTestTransformer.transform(any(Action.class))).thenReturn(Observable.<Result>empty());

        //
        //Act
        //
        testObserver = nowPlayingViewModel.getUiModels().test();
        testScheduler.triggerActions();

        //
        //Assert
        //
        testObserver.assertNoErrors();
        testObserver.assertValueCount(1);

        UiModel uiModel = (UiModel) testObserver.getEvents().get(0).get(0);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isTrue();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.DO_NOTHING);
        assertThat(uiModel.getCurrentList()).isEmpty();
        assertThat(uiModel.getResultList()).isNull();
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isFalse();
        assertThat(uiModel.getPageNumber()).isEqualTo(0);
    }

    @Test
    public void inFlightState() {
        //
        //Arrange
        //
        TestObserver<UiModel> testObserver;
        TestNowPlayingViewModel nowPlayingViewModel = new TestNowPlayingViewModel(mockApplication,
                mockServiceGateway, mockNowPlayingInteractor);
        nowPlayingViewModel.init(null);

        final int pageNumber = 1;
        ScrollResult scrollResult = ScrollResult.inFlight(pageNumber);

        ArgumentCaptor<Action> argumentCaptor = ArgumentCaptor.forClass(Action.class);
        when(mockTestTransformer.transform(argumentCaptor.capture())).thenReturn(Observable.just((Result) scrollResult));


        //
        //Act
        //
        testObserver = nowPlayingViewModel.getUiModels().test();
        testScheduler.triggerActions();

        //
        //Assert
        //
        //Observer Test
        testObserver.assertNoErrors();
        testObserver.assertValueCount(2);

        //Model Test
        UiModel uiModel = (UiModel) testObserver.getEvents().get(0).get(1);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isTrue();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.DO_NOTHING);
        assertThat(uiModel.getCurrentList()).isEmpty();
        assertThat(uiModel.getResultList()).isNull();
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isFalse();
        assertThat(uiModel.getPageNumber()).isEqualTo(pageNumber);

        //Action translation test
        Action action = argumentCaptor.getValue();
        assertThat(action).isNotNull();
        assertThat(action).isInstanceOf(ScrollAction.class);

        ScrollAction scrollAction = (ScrollAction) action;
        assertThat(scrollAction.getPageNumber()).isEqualTo(pageNumber);
    }

    @Test
    public void inSuccessState() {
        //
        //Arrange
        //
        TestObserver<UiModel> testObserver;
        TestNowPlayingViewModel nowPlayingViewModel = new TestNowPlayingViewModel(mockApplication,
                mockServiceGateway, mockNowPlayingInteractor);
        nowPlayingViewModel.init(null);

        final int pageNumber = 1;

        ScrollResult scrollResultInFlight = ScrollResult.inFlight(pageNumber);

        List<MovieInfo> movieInfoList = new ArrayList<>();
        movieInfoList.add(movieInfo);

        ScrollResult scrollResultSuccess = ScrollResult.sucess(pageNumber, movieInfoList);

        ArgumentCaptor<Action> argumentCaptor = ArgumentCaptor.forClass(Action.class);
        when(mockTestTransformer.transform(argumentCaptor.capture())).thenReturn(Observable.just(
                (Result) scrollResultInFlight,
                (Result) scrollResultSuccess));

        //
        //Act
        //
        testObserver = nowPlayingViewModel.getUiModels().test();
        testScheduler.triggerActions();

        //
        //Assert
        //
        //Observer Test
        testObserver.assertNoErrors();
        testObserver.assertValueCount(3);

        //Model Test
        UiModel uiModel = (UiModel) testObserver.getEvents().get(0).get(2);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isFalse();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.ADD_DATA_REMOVE_IN_PROGRESS);
        assertThat(uiModel.getCurrentList()).isNotEmpty();
        assertThat(uiModel.getCurrentList()).hasSize(1);
        assertThat(uiModel.getResultList()).isNotEmpty();
        assertThat(uiModel.getResultList()).hasSize(1);
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isTrue();
        assertThat(uiModel.getPageNumber()).isEqualTo(pageNumber);

        //Test List Data
        MovieViewInfo movieViewInfo = uiModel.getResultList().get(0);
        assertThat(movieViewInfo.getPictureUrl()).isEqualToIgnoringCase(movieInfo.getPictureUrl());
        assertThat(movieViewInfo.getTitle()).isEqualToIgnoringCase(movieInfo.getTitle());
        assertThat(movieViewInfo.getRating()).isEqualToIgnoringCase(String.valueOf(Math.round(movieInfo.getRating()) + "/10"));
        assertThat(movieViewInfo.isHighRating()).isTrue();

    }

    @Test
    public void inRestoreState() {
        //
        //Arrange
        //
        TestObserver<UiModel> testObserver;
        TestNowPlayingViewModel nowPlayingViewModel = new TestNowPlayingViewModel(mockApplication,
                mockServiceGateway, mockNowPlayingInteractor);

        //restore activity
        final int pageNumber = 2;
        UiModel restoreState = UiModel.restoreState(pageNumber, null, null);
        nowPlayingViewModel.init(restoreState);

        //Fake Data from Restore
        List<MovieInfo> movieInfoList1 = new ArrayList<>();
        movieInfoList1.add(movieInfo);
        List<MovieInfo> movieInfoList2 = new ArrayList<>();
        movieInfoList2.add(movieInfo);

        RestoreResult restoreResult_inFlight_1 = RestoreResult.inFlight(1, null);
        RestoreResult restoreResult_inFlight_1_success = RestoreResult.inFlight(1, movieInfoList1);
        RestoreResult restoreResult_inFlight_2 = RestoreResult.inFlight(2, null);
        RestoreResult restoreResult_success_2 = RestoreResult.sucess(2, movieInfoList2);

        ArgumentCaptor<Action> argumentCaptor = ArgumentCaptor.forClass(Action.class);
        when(mockTestTransformer.transform(argumentCaptor.capture())).thenReturn(Observable.just(
                (Result) restoreResult_inFlight_1,
                (Result) restoreResult_inFlight_1_success,
                (Result) restoreResult_inFlight_2,
                (Result) restoreResult_success_2));

        //
        //Act
        //
        testObserver = nowPlayingViewModel.getUiModels().test();
        testScheduler.triggerActions();

        //
        //Assert
        //
        //Observer Test
        testObserver.assertNoErrors();
        testObserver.assertValueCount(5);

        //Model Test 1st Item
        UiModel uiModel = (UiModel) testObserver.getEvents().get(0).get(0);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isTrue();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.DO_NOTHING);
        assertThat(uiModel.getCurrentList()).isEmpty();
        assertThat(uiModel.getResultList()).isNullOrEmpty();
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isFalse();
        assertThat(uiModel.getPageNumber()).isEqualTo(0);

        //Model Test 2nd Item
        uiModel = (UiModel) testObserver.getEvents().get(0).get(1);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isTrue();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.DO_NOTHING);
        assertThat(uiModel.getCurrentList()).isEmpty();
        assertThat(uiModel.getResultList()).isNullOrEmpty();
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isFalse();
        assertThat(uiModel.getPageNumber()).isEqualTo(1);

        //Model Test 3rd Item
        uiModel = (UiModel) testObserver.getEvents().get(0).get(2);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isTrue();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.ADD_DATA_ONLY);
        assertThat(uiModel.getCurrentList()).isNotEmpty();
        assertThat(uiModel.getCurrentList()).hasSize(1);
        assertThat(uiModel.getResultList()).isNotEmpty();
        assertThat(uiModel.getResultList()).hasSize(1);
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isFalse();
        assertThat(uiModel.getPageNumber()).isEqualTo(1);

        MovieViewInfo movieViewInfo = uiModel.getResultList().get(0);
        assertThat(movieViewInfo.getPictureUrl()).isEqualToIgnoringCase(movieInfo.getPictureUrl());
        assertThat(movieViewInfo.getTitle()).isEqualToIgnoringCase(movieInfo.getTitle());
        assertThat(movieViewInfo.getRating()).isEqualToIgnoringCase(String.valueOf(Math.round(movieInfo.getRating()) + "/10"));
        assertThat(movieViewInfo.isHighRating()).isTrue();

        //Model Test 4th Item
        uiModel = (UiModel) testObserver.getEvents().get(0).get(3);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isTrue();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.DO_NOTHING);
        assertThat(uiModel.getCurrentList()).isNotEmpty();
        assertThat(uiModel.getCurrentList()).hasSize(1);
        assertThat(uiModel.getResultList()).isNullOrEmpty();
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isFalse();
        assertThat(uiModel.getPageNumber()).isEqualTo(pageNumber);

        //Model Test 5th Item
        uiModel = (UiModel) testObserver.getEvents().get(0).get(4);
        assertThat(uiModel).isNotNull();
        assertThat(uiModel.isFirstTimeLoad()).isFalse();
        assertThat(uiModel.getAdapterCommandType()).isEqualTo(AdapterCommandType.ADD_DATA_ONLY);
        assertThat(uiModel.getCurrentList()).isNotEmpty();
        assertThat(uiModel.getCurrentList()).hasSize(2);
        assertThat(uiModel.getResultList()).isNotEmpty();
        assertThat(uiModel.getResultList()).hasSize(1);
        assertThat(uiModel.getFailureMsg()).isNull();
        assertThat(uiModel.isEnableScrollListener()).isTrue();
        assertThat(uiModel.getPageNumber()).isEqualTo(pageNumber);

        //test result
        movieViewInfo = uiModel.getResultList().get(0);
        assertThat(movieViewInfo.getPictureUrl()).isEqualToIgnoringCase(movieInfo.getPictureUrl());
        assertThat(movieViewInfo.getTitle()).isEqualToIgnoringCase(movieInfo.getTitle());
        assertThat(movieViewInfo.getRating()).isEqualToIgnoringCase(String.valueOf(Math.round(movieInfo.getRating()) + "/10"));
        assertThat(movieViewInfo.isHighRating()).isTrue();

        //test full list
        movieViewInfo = uiModel.getCurrentList().get(0);
        assertThat(movieViewInfo.getPictureUrl()).isEqualToIgnoringCase(movieInfo.getPictureUrl());
        assertThat(movieViewInfo.getTitle()).isEqualToIgnoringCase(movieInfo.getTitle());
        assertThat(movieViewInfo.getRating()).isEqualToIgnoringCase(String.valueOf(Math.round(movieInfo.getRating()) + "/10"));
        assertThat(movieViewInfo.isHighRating()).isTrue();

        movieViewInfo = uiModel.getCurrentList().get(1);
        assertThat(movieViewInfo.getPictureUrl()).isEqualToIgnoringCase(movieInfo.getPictureUrl());
        assertThat(movieViewInfo.getTitle()).isEqualToIgnoringCase(movieInfo.getTitle());
        assertThat(movieViewInfo.getRating()).isEqualToIgnoringCase(String.valueOf(Math.round(movieInfo.getRating()) + "/10"));
        assertThat(movieViewInfo.isHighRating()).isTrue();


//
//        //Test List Data
//        MovieViewInfo movieViewInfo = uiModel.getResultList().get(0);
//        assertThat(movieViewInfo.getPictureUrl()).isEqualToIgnoringCase(movieInfo.getPictureUrl());
//        assertThat(movieViewInfo.getTitle()).isEqualToIgnoringCase(movieInfo.getTitle());
//        assertThat(movieViewInfo.getRating()).isEqualToIgnoringCase(String.valueOf(Math.round(movieInfo.getRating()) + "/10"));
//        assertThat(movieViewInfo.isHighRating()).isTrue();
    }

    private class TestTransformer {
        Observable<Result> transform(Action action) {
            return Observable.empty();
        }
    }
}