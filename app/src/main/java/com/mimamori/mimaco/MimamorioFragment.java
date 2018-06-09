package com.mimamori.mimaco;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import otoshimono.com.lost.mamorio.sdk.Mamorio;
import otoshimono.com.lost.mamorio.sdk.MamorioSDK;
import otoshimono.com.lost.mamorio.sdk.User;
import otoshimono.com.lost.mamorio.sdk.Error;


public class MimamorioFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = MimamorioFragment.class.getName();

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private KEYS keys = new KEYS();

    private String APP_TOKEN = "APP_TOKEN";
    private PointManager pointManager = new PointManager();
    private String MY_USERNAME = "1";

    public MimamorioFragment() {
        // Required empty public constructor

    }


    @Override
    public void onStart(){
        super.onStart();
        Log.i("LifeCycle", "onStart");

        // ACCESS_FINE_LOCATIONの許可(Android 6.0以上向け）
        if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            MamorioSDK.setUp(getActivity().getBaseContext(), APP_TOKEN); // 探索専用
            step1();
        }

        try {
//            Log.d("resultJSON:", pointManager.getAllUser().getJSONObject(0).getString("email"));
//            Log.d(TAG, pointManager.addPoint(4, 1).getString("point"));
//            Log.d("getUser", pointManager.getUser(2).getString("username"));
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MimamorioFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MimamorioFragment newInstance(String param1, String param2) {
        MimamorioFragment fragment = new MimamorioFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }


    void step1()
    {
        Log.d(TAG,"step1()");

        User.signUpAsTemporalUser(new User.UserCallback() {
            @Override
            public void onSuccess(User user) {
                //アカウント登録成功時の処理
                Log.d(TAG,"捜索専用ユーザーの登録に成功しました。ペアリングは行わず、周囲を高精度で探索し続け「みんなでさがす」に貢献します。");
                step2();
            }

            @Override
            public void onError(Error error) {
                //アカウント登録失敗時の処理
                Log.d(TAG,"捜索専用ユーザーの登録に失敗しました。エラーメッセージ：" + error.getMessage());
            }
        });
    }

    void step2()
    {
        Log.d(TAG,"step2()");

        MamorioSDK.rangingStart(
                new MamorioSDK.RangingInitializeCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG,"レンジングの開始に成功しました。");
                    }
                    @Override
                    public void onError(Error error) {
                        Log.d(TAG,"Sレンジングの開始に失敗しました。エラーメッセージ："+error.getMessage());
                    }
                },
                //MAMORIO発見時のコールバック（null可）
                new MamorioSDK.RangingCallbackEnter() {
                    @Override
                    public void onEnter(List<Mamorio> list) {
                        //list:発見したMAMORIOの一覧
                        //nullの場合コールバックしない
                        Log.d(TAG,"RangingCallbackEnter()");

                        for(int i = 0 ; i < list.size() ; i++) {
                            Mamorio dev = list.get(i);

                            String name = dev.getName();
                            if(name != null){
                                Log.d(TAG,"name=" + name);
                            }

                            int major = dev.getMajor();
                            int minor = dev.getMinor();
                            Log.d(TAG,major + "," + minor);

                            if(dev.isNotYours() == true) {
                                Log.d(TAG,"他人のMAMORIO");
                                pointManager.addPoint(MY_USERNAME, 1);
                            } else {
                                Log.d(TAG,"自分のMAMORIO");
                            }
                        }
                    }
                },
                //MAMORIO紛失時のコールバック(null可)
                new MamorioSDK.RangingCallbackExit() {
                    @Override
                    public void onExit(List<Mamorio> list) {
                        //list:紛失した自分のMAMORIO一覧
                        Log.d(TAG,"RangingCallbackExit()");

                        for(int i = 0 ; i < list.size() ; i++) {
                            Mamorio dev = list.get(i);

                            String name = dev.getName();
                            if(name != null){
                                Log.d(TAG,"name=" + name);
                            }

                            int major = dev.getMajor();
                            int minor = dev.getMinor();
                            Log.d(TAG,major + "," + minor);

                            if(dev.isNotYours() == true) {
                                Log.d(TAG,"他人のMAMORIO");
                            } else {
                                Log.d(TAG,"自分のMAMORIO");
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"onStop()");
        MamorioSDK.rangingStop();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mimamorio, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
