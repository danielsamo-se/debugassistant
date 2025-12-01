import SkeletonHistoryItem from './SkeletonHistoryItem';

export default function SkeletonHistoryList() {
  return (
    <div className="space-y-4">
      <SkeletonHistoryItem />
      <SkeletonHistoryItem />
      <SkeletonHistoryItem />
      <SkeletonHistoryItem />
    </div>
  );
}
