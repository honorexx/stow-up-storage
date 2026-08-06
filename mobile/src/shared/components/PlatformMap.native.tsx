import { forwardRef } from 'react';
import MapView, { MapViewProps, Marker, Region } from 'react-native-maps';

export type { Region };
export { Marker };
export type PlatformMapHandle = MapView;

export const PlatformMap = forwardRef<MapView, MapViewProps>((props, ref) => (
  <MapView ref={ref} {...props} />
));
