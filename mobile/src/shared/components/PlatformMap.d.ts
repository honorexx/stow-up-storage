import type { ComponentType, PropsWithChildren } from 'react';
import type { StyleProp, ViewStyle } from 'react-native';

export type Region = {
  latitude: number;
  longitude: number;
  latitudeDelta: number;
  longitudeDelta: number;
};

export type PlatformMapHandle = {
  animateToRegion: (region: Region, duration?: number) => void;
};

export const PlatformMap: ComponentType<
  PropsWithChildren<{
    ref?: React.Ref<PlatformMapHandle>;
    style?: StyleProp<ViewStyle>;
    initialRegion?: Region;
    onRegionChangeComplete?: (region: Region) => void;
    showsUserLocation?: boolean;
    showsMyLocationButton?: boolean;
  }>
>;

export const Marker: ComponentType<
  PropsWithChildren<{
    coordinate: { latitude: number; longitude: number };
    onPress?: () => void;
    title?: string;
    description?: string;
  }>
>;
