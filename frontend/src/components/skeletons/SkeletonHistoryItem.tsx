export default function SkeletonHistoryItem() {
  return (
    <div className="p-4 bg-slate-800 rounded-xl border border-slate-700 animate-pulse">
      <div className="flex justify-between mb-3">
        <div className="flex gap-2">
          <div className="h-4 w-16 bg-slate-700/40 rounded" />
          <div className="h-4 w-24 bg-slate-700/40 rounded" />
        </div>

        <div className="h-4 w-20 bg-slate-700/40 rounded" />
      </div>

      <div className="h-20 bg-slate-700/40 rounded" />
    </div>
  );
}
