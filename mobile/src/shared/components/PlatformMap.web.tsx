import 'leaflet/dist/leaflet.css';

import L from 'leaflet';
import {
  forwardRef,
  MutableRefObject,
  PropsWithChildren,
  useEffect,
  useImperativeHandle,
  useRef,
} from 'react';
import { MapContainer, Marker as LeafletMarker, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import { StyleProp, View, ViewStyle } from 'react-native';

export type Region = {
  latitude: number;
  longitude: number;
  latitudeDelta: number;
  longitudeDelta: number;
};
export type PlatformMapHandle = { animateToRegion: (region: Region, duration?: number) => void };

type MapProps = PropsWithChildren<{
  style?: StyleProp<ViewStyle>;
  initialRegion?: Region;
  onRegionChangeComplete?: (region: Region) => void;
  showsUserLocation?: boolean;
  showsMyLocationButton?: boolean;
}>;

function zoomFromDelta(delta = 0.13) {
  return Math.max(3, Math.min(18, Math.round(Math.log2(360 / Math.max(delta, 0.002)))));
}

function MapBridge({
  controller,
  onRegionChangeComplete,
}: {
  controller: MutableRefObject<L.Map | null>;
  onRegionChangeComplete?: (region: Region) => void;
}) {
  const map = useMap();
  controller.current = map;
  useMapEvents({
    moveend() {
      const center = map.getCenter();
      const bounds = map.getBounds();
      onRegionChangeComplete?.({
        latitude: center.lat,
        longitude: center.lng,
        latitudeDelta: Math.abs(bounds.getNorth() - bounds.getSouth()),
        longitudeDelta: Math.abs(bounds.getEast() - bounds.getWest()),
      });
    },
  });
  useEffect(() => () => void (controller.current = null), [controller]);
  return null;
}

export const PlatformMap = forwardRef<PlatformMapHandle, MapProps>(
  ({ style, children, initialRegion, onRegionChangeComplete }, ref) => {
    const controller = useRef<L.Map | null>(null);
    const start = initialRegion ?? {
      latitude: -23.5505,
      longitude: -46.6333,
      latitudeDelta: 0.13,
      longitudeDelta: 0.13,
    };

    useImperativeHandle(ref, () => ({
      animateToRegion(next, duration = 500) {
        controller.current?.flyTo([next.latitude, next.longitude], zoomFromDelta(next.latitudeDelta), {
          duration: Math.max(0.2, duration / 1000),
        });
      },
    }));

    return (
      <View style={[{ overflow: 'hidden', backgroundColor: '#E8EEF5' }, style]}>
        <MapContainer
          center={[start.latitude, start.longitude]}
          zoom={zoomFromDelta(start.latitudeDelta)}
          scrollWheelZoom
          style={{ width: '100%', height: '100%', minHeight: 300 }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapBridge controller={controller} onRegionChangeComplete={onRegionChangeComplete} />
          {children}
        </MapContainer>
      </View>
    );
  },
);

export function Marker({
  coordinate,
  onPress,
  title,
  description,
}: PropsWithChildren<{
  coordinate: { latitude: number; longitude: number };
  onPress?: () => void;
  title?: string;
  description?: string;
}>) {
  const icon = L.divIcon({
    className: '',
    html: '<div style="width:18px;height:18px;border:3px solid white;border-radius:50%;background:#ff5a14;box-shadow:0 3px 10px #0b2b4f66"></div>',
    iconSize: [18, 18],
    iconAnchor: [9, 9],
  });
  return (
    <LeafletMarker
      position={[coordinate.latitude, coordinate.longitude]}
      icon={icon}
      eventHandlers={{ click: () => onPress?.() }}
    >
      {(title || description) && (
        <Popup>
          <strong>{title}</strong>
          {description ? <div>{description}</div> : null}
        </Popup>
      )}
    </LeafletMarker>
  );
}
