package com.gemserk.tools.animationeditor.core;

import java.util.ArrayList;
import java.util.HashMap;

public class AnimationUtils {
	
	static JointConverter jointConverter = JointConverter.instance;

	public static SkeletonAnimationKeyFrame keyFrame(String name, Skeleton skeleton, float time) {
		HashMap<String, float[]> jointKeyFrames = new HashMap<String, float[]>();
		
		ArrayList<Joint> jointList = JointUtils.toArrayList(skeleton.getRoot());
		
		for (Joint joint : jointList) {
			jointKeyFrames.put(joint.getId(), jointConverter.copyFromObject(joint, null));
		}
		
		return new SkeletonAnimationKeyFrame(name, skeleton, time, jointKeyFrames);
	}

	public static void updateKeyFrame(Skeleton skeleton, SkeletonAnimationKeyFrame keyframe) {
		keyframe.setSkeleton(JointUtils.cloneSkeleton(skeleton));
	}

	/**
	 * Modifies the Skeleton to matches the keyframe.
	 * 
	 * @param skeleton
	 *            The Skeleton to be modified.
	 * @param keyframe
	 *            The keyframe of the skeleton.
	 */
	public static void setKeyframeToSkeleton(Skeleton skeleton, SkeletonAnimationKeyFrame keyframe) {
		ArrayList<Joint> joints = JointUtils.toArrayList(skeleton.getRoot());

		Skeleton keyFrameSkeleton = keyframe.getSkeleton();

		for (int i = 0; i < joints.size(); i++) {
			Joint joint = joints.get(i);
			Joint keyFrameJointValue = keyFrameSkeleton.getRoot().find(joint.getId());
			if (keyFrameJointValue == null)
				continue;
			joint.setLocalPosition(keyFrameJointValue.getLocalX(), keyFrameJointValue.getLocalY());
			joint.setLocalAngle(keyFrameJointValue.getLocalAngle());
		}

	}

}
