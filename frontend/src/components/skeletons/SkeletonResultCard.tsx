export default function SkeletonResultCard() {
  return (
    <div className="p-5 bg-slate-800 border border-slate-700 rounded-lg animate-pulse">
      <div className="flex justify-between mb-3">
        <div className="flex gap-2">
          <div className="h-4 w-20 bg-slate-700/40 rounded" />
          <div className="h-4 w-16 bg-slate-700/40 rounded" />
        </div>
        <div className="h-4 w-12 bg-slate-700/40 rounded" />
      </div>

      <div className="h-5 w-3/4 bg-slate-700/40 rounded mb-2" />
      <div className="h-5 w-1/2 bg-slate-700/40 rounded mb-4" />

      <div className="h-4 w-24 bg-slate-700/40 rounded" />
    </div>
  );
}
